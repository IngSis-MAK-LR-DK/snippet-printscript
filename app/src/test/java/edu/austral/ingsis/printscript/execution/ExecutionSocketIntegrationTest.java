package edu.austral.ingsis.printscript.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"AUTH0_DOMAIN=example.auth0.com", "AUTH0_AUDIENCE=api"})
class ExecutionSocketIntegrationTest {

    @LocalServerPort private int port;

    @MockitoBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void acceptAnyToken() {
        when(jwtDecoder.decode(anyString()))
                .thenReturn(
                        new Jwt(
                                "token",
                                Instant.now(),
                                Instant.now().plusSeconds(60),
                                Map.of("alg", "none"),
                                Map.of("sub", "auth0|handler")));
    }

    @Test
    void unaEjecucionInteractivaDePuntaAPunta() throws Exception {
        Client client = connect("Bearer token");

        client.send(
                "{\"type\":\"start\",\"version\":\"1.1\",\"interactive\":true,"
                        + "\"content\":\"let a: string = readInput(\\\"nombre\\\");"
                        + " println(\\\"hola \\\" + a);\"}");
        assertThat(client.next()).contains("\"type\":\"input\"").contains("nombre");
        client.send("{\"type\":\"input\",\"value\":\"Ana\"}");

        assertThat(client.next()).contains("\"line\":\"hola Ana\"");
        assertThat(client.next()).contains("\"type\":\"done\"").contains("\"errors\":[]");
        assertThat(client.closed.get(5, TimeUnit.SECONDS)).isEqualTo(WebSocket.NORMAL_CLOSURE);
    }

    @Test
    void unMensajeQueNoEsJsonCierraLaConexion() throws Exception {
        Client client = connect("Bearer token");

        client.send("no es json");

        assertThat(client.closed.get(5, TimeUnit.SECONDS)).isEqualTo(1008);
    }

    @Test
    void unTipoDesconocidoCierraLaConexion() throws Exception {
        Client client = connect("Bearer token");

        client.send("{\"type\":\"otra-cosa\"}");

        assertThat(client.closed.get(5, TimeUnit.SECONDS)).isEqualTo(1008);
    }

    @Test
    void unCodigoVacioCierraLaConexion() throws Exception {
        Client client = connect("Bearer token");

        client.send("{\"type\":\"start\",\"content\":\"\"}");

        assertThat(client.closed.get(5, TimeUnit.SECONDS)).isEqualTo(1008);
    }

    @Test
    void sinTokenNoSeAbreElWebSocket() {
        assertThatThrownBy(() -> connect(null))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(WebSocketHandshakeException.class);
    }

    private Client connect(String authorization) throws Exception {
        Client client = new Client();
        WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        client.socket =
                builder.buildAsync(URI.create("ws://localhost:" + port + "/execute/stream"), client)
                        .get(5, TimeUnit.SECONDS);
        return client;
    }

    private static final class Client implements WebSocket.Listener {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final CompletableFuture<Integer> closed = new CompletableFuture<>();
        private final StringBuilder partial = new StringBuilder();
        private WebSocket socket;

        void send(String text) {
            socket.sendText(text, true).join();
        }

        String next() throws InterruptedException {
            String message = messages.poll(5, TimeUnit.SECONDS);
            assertThat(message).as("llego un mensaje").isNotNull();
            return message;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(statusCode);
            return null;
        }
    }
}
