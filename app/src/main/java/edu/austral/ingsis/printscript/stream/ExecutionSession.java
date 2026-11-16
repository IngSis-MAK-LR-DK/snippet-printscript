package edu.austral.ingsis.printscript.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.austral.ingsis.printscript.common.Version;
import edu.austral.ingsis.printscript.language.LanguageService;
import edu.austral.ingsis.printscript.language.MissingInputException;
import edu.austral.ingsis.printscript.web.Versions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Una ejecucion en streaming (US 9 y 10). Cada println sale como {@code output} apenas se ejecuta.
 * Cada readInput usa el siguiente de los inputs que vinieron al empezar; si no quedan y la
 * ejecucion es interactiva, se pide con un mensaje {@code input} y el programa espera la respuesta.
 * Al terminar manda {@code done} con los errores (vacio si anduvo) y cierra.
 *
 * <p>El programa corre en su propio hilo: el que recibe los mensajes queda libre para entregar los
 * inputs. Una sola ejecucion por sesion.
 */
public class ExecutionSession {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionSession.class);

    /** Lo que se pone en la cola de inputs para despertar al programa cuando se corta la sesion. */
    private static final String CANCELLED = new String("cancelled");

    private final LanguageService language;
    private final MessageSink sink;
    private final Executor executor;
    private final Duration inputTimeout;
    private final BlockingQueue<String> inputs = new LinkedBlockingQueue<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile boolean cancelled;

    public ExecutionSession(
            LanguageService language, MessageSink sink, Executor executor, Duration inputTimeout) {
        this.language = language;
        this.sink = sink;
        this.executor = executor;
        this.inputTimeout = inputTimeout;
    }

    /**
     * Empieza la ejecucion. Si la version no existe, o ya se empezo, contesta {@code done} con el
     * error sin correr nada.
     */
    public void start(
            String code, String version, List<String> initialInputs, boolean interactive) {
        if (!started.compareAndSet(false, true)) {
            sink.send(Map.of("type", "error", "message", "La ejecucion ya empezo"));
            return;
        }
        Version parsed;
        try {
            parsed = Versions.parse(version);
        } catch (IllegalArgumentException e) {
            finish(List.of(e.getMessage()));
            return;
        }
        List<String> given = initialInputs != null ? initialInputs : List.of();
        executor.execute(() -> run(code, parsed, given.iterator(), interactive));
    }

    /** Un valor que escribio el usuario para el readInput que esta esperando. */
    public void input(String value) {
        inputs.offer(value != null ? value : "");
    }

    /** Se corto la conexion: el programa deja de esperar inputs y no manda nada mas. */
    public void cancel() {
        cancelled = true;
        inputs.offer(CANCELLED);
    }

    private void run(String code, Version version, Iterator<String> given, boolean interactive) {
        List<String> errors;
        try {
            errors =
                    language.run(
                            code,
                            version,
                            line -> send(Map.of("type", "output", "line", line)),
                            prompt -> read(prompt, given, interactive),
                            null);
        } catch (StoppedException e) {
            errors = List.of(e.getMessage());
        } catch (RuntimeException e) {
            // Un error de la libreria que no es del programa: no se le muestra el detalle.
            LOG.warn("La ejecucion fallo por un error interno", e);
            errors = List.of("Error interno al ejecutar el snippet");
        }
        finish(errors);
    }

    private String read(String prompt, Iterator<String> given, boolean interactive) {
        if (given.hasNext()) {
            return given.next();
        }
        if (!interactive) {
            throw new MissingInputException(prompt);
        }
        send(Map.of("type", "input", "prompt", prompt != null ? prompt : ""));
        String value;
        try {
            value = inputs.poll(inputTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoppedException("La ejecucion se interrumpio");
        }
        if (value == null) {
            throw new StoppedException(
                    "No llego el input en " + inputTimeout.toSeconds() + " segundos");
        }
        if (value == CANCELLED) {
            throw new StoppedException("Se corto la conexion");
        }
        return value;
    }

    /** Avisa como termino, si todavia hay a quien, y cierra siempre. */
    private void finish(List<String> errors) {
        try {
            if (!cancelled) {
                sink.send(Map.of("type", "done", "errors", new ArrayList<>(errors)));
            }
        } catch (RuntimeException e) {
            LOG.debug("No se pudo avisar el fin de la ejecucion", e);
        } finally {
            sink.close();
        }
    }

    private void send(Map<String, Object> message) {
        if (cancelled) {
            throw new StoppedException("Se corto la conexion");
        }
        sink.send(message);
    }

    /** La ejecucion se corto desde afuera (sin input a tiempo, o la conexion se cerro). */
    static final class StoppedException extends RuntimeException {
        StoppedException(String message) {
            super(message);
        }
    }
}
