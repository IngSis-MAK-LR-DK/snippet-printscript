package edu.austral.ingsis.printscript.stream;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import edu.austral.ingsis.printscript.language.LanguageService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * {@code /execute/stream}: la ejecucion en streaming por WebSocket. El cliente (el handler) manda
 * {@code start} con el codigo y despues un {@code input} por cada valor que pide el programa. El
 * protocolo esta en el contrato del adaptador (snippet-handler, docs/language-adapter-contract.md).
 */
public class ExecutionSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionSocketHandler.class);
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_LIMIT_BYTES = 512 * 1024;

    private final LanguageService language;
    private final ObjectMapper json;
    private final Executor executor;
    private final Duration inputTimeout;
    private final int maxCodeLength;
    private final Map<String, ExecutionSession> sessions = new ConcurrentHashMap<>();

    public ExecutionSocketHandler(
            LanguageService language,
            ObjectMapper json,
            Executor executor,
            Duration inputTimeout,
            int maxCodeLength) {
        this.language = language;
        this.json = json;
        this.executor = executor;
        this.inputTimeout = inputTimeout;
        this.maxCodeLength = maxCodeLength;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // El programa manda desde su hilo y el que atiende la sesion tambien puede mandar: el
        // decorador serializa los envios.
        WebSocketSession safe =
                new ConcurrentWebSocketSessionDecorator(
                        session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES);
        sessions.put(
                session.getId(),
                new ExecutionSession(language, sink(safe), executor, inputTimeout));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException {
        ExecutionSession execution = sessions.get(session.getId());
        JsonNode body;
        try {
            body = json.readTree(message.getPayload());
        } catch (JsonProcessingException e) {
            reject(session, "El mensaje no es un JSON valido");
            return;
        }
        switch (body.path("type").asText()) {
            case "start" -> start(session, execution, body);
            case "input" -> execution.input(body.path("value").asText());
            default -> reject(session, "Tipo de mensaje desconocido");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ExecutionSession execution = sessions.remove(session.getId());
        if (execution != null) {
            execution.cancel();
        }
    }

    private void start(WebSocketSession session, ExecutionSession execution, JsonNode body)
            throws IOException {
        String code = body.path("content").asText("");
        if (code.isBlank() || code.length() > maxCodeLength) {
            reject(session, "El codigo esta vacio o es demasiado largo");
            return;
        }
        List<String> inputs = new ArrayList<>();
        body.path("inputs").forEach(value -> inputs.add(value.asText()));
        JsonNode version = body.path("version");
        execution.start(
                code,
                version.isTextual() ? version.asText() : null,
                inputs,
                body.path("interactive").asBoolean(false));
    }

    private static void reject(WebSocketSession session, String reason) throws IOException {
        session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
    }

    private MessageSink sink(WebSocketSession session) {
        return new MessageSink() {
            @Override
            public void send(Map<String, Object> message) {
                try {
                    session.sendMessage(new TextMessage(json.writeValueAsString(message)));
                } catch (IOException e) {
                    throw new IllegalStateException("No se pudo mandar el mensaje", e);
                }
            }

            @Override
            public void close() {
                try {
                    session.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    LOG.debug("La sesion ya estaba cerrada", e);
                }
            }
        };
    }
}
