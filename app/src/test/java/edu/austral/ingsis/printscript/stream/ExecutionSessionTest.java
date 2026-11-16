package edu.austral.ingsis.printscript.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.austral.ingsis.printscript.language.LanguageService;

import org.junit.jupiter.api.Test;

class ExecutionSessionTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    private final LanguageService language = new LanguageService();
    private final RecordingSink sink = new RecordingSink();

    @Test
    void cadaPrintlnSaleComoUnMensajeYAlFinalDone() throws InterruptedException {
        session(WAIT).start("println(\"hola\");\nprintln(1 + 2);", "1.1", List.of(), false);

        assertThat(sink.next()).isEqualTo(Map.of("type", "output", "line", "hola"));
        assertThat(sink.next()).isEqualTo(Map.of("type", "output", "line", "3"));
        assertThat(sink.next()).isEqualTo(Map.of("type", "done", "errors", List.of()));
        assertThat(sink.closed.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void usaPrimeroLosInputsQueVinieronAlEmpezar() throws InterruptedException {
        session(WAIT)
                .start(
                        "let a: string = readInput(\"a\");\nprintln(a);",
                        "1.1",
                        List.of("Ana"),
                        false);

        assertThat(sink.next()).isEqualTo(Map.of("type", "output", "line", "Ana"));
        assertThat(sink.next()).containsEntry("type", "done");
    }

    @Test
    void sinInputsYNoInteractivaTerminaConError() throws InterruptedException {
        session(WAIT).start("let a: string = readInput(\"a\");", "1.1", List.of(), false);

        Map<String, Object> done = sink.next();
        assertThat(done).containsEntry("type", "done");
        assertThat((List<?>) done.get("errors")).hasSize(1);
    }

    @Test
    void interactivaPideElInputYSigueConLoQueLlega() throws InterruptedException {
        ExecutionSession session = session(WAIT);
        session.start(
                "println(\"antes\");\nlet a: string = readInput(\"nombre\");\nprintln(a);",
                "1.1",
                List.of(),
                true);

        assertThat(sink.next()).isEqualTo(Map.of("type", "output", "line", "antes"));
        assertThat(sink.next()).isEqualTo(Map.of("type", "input", "prompt", "nombre"));
        session.input("Beto");
        assertThat(sink.next()).isEqualTo(Map.of("type", "output", "line", "Beto"));
        assertThat(sink.next()).isEqualTo(Map.of("type", "done", "errors", List.of()));
    }

    @Test
    void siElInputNoLlegaATiempoTerminaConError() throws InterruptedException {
        session(Duration.ofMillis(50))
                .start("let a: string = readInput(\"x\");", "1.1", List.of(), true);

        assertThat(sink.next()).containsEntry("type", "input");
        Map<String, Object> done = sink.next();
        assertThat(done.get("errors").toString()).contains("No llego el input");
    }

    @Test
    void siSeCortaLaConexionDejaDeEsperar() throws InterruptedException {
        ExecutionSession session = session(WAIT);
        session.start("let a: string = readInput(\"x\");", "1.1", List.of(), true);
        assertThat(sink.next()).containsEntry("type", "input");

        session.cancel();

        assertThat(sink.closed.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void unaVersionQueNoExisteTerminaSinCorrer() throws InterruptedException {
        session(WAIT).start("println(1);", "9.9", List.of(), false);

        Map<String, Object> done = sink.next();
        assertThat(done).containsEntry("type", "done");
        assertThat(done.get("errors").toString()).contains("9.9");
    }

    @Test
    void elCodigoInvalidoTerminaConElErrorDelPrograma() throws InterruptedException {
        session(WAIT).start("println(;", "1.1", List.of(), false);

        Map<String, Object> done = sink.next();
        assertThat((List<?>) done.get("errors")).hasSize(1);
    }

    @Test
    void noSePuedeEmpezarDosVeces() throws InterruptedException {
        ExecutionSession session = session(WAIT);
        session.start("let a: string = readInput(\"x\");", "1.1", List.of(), true);
        assertThat(sink.next()).containsEntry("type", "input");

        session.start("println(1);", "1.1", List.of(), false);

        assertThat(sink.next()).containsEntry("type", "error");
        session.cancel();
    }

    private ExecutionSession session(Duration inputTimeout) {
        return new ExecutionSession(
                language, sink, Executors.newVirtualThreadPerTaskExecutor(), inputTimeout);
    }

    private static final class RecordingSink implements MessageSink {
        private final BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();
        private final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public void send(Map<String, Object> message) {
            messages.add(message);
        }

        @Override
        public void close() {
            closed.countDown();
        }

        Map<String, Object> next() throws InterruptedException {
            Map<String, Object> message = messages.poll(5, TimeUnit.SECONDS);
            assertThat(message).as("llego un mensaje").isNotNull();
            return message;
        }
    }
}
