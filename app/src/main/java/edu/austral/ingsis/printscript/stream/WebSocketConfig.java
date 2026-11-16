package edu.austral.ingsis.printscript.stream;

import java.time.Duration;
import java.util.concurrent.Executors;

import edu.austral.ingsis.printscript.language.LanguageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registra {@code /execute/stream}. El handshake es un GET normal: pasa por Spring Security y exige
 * el mismo JWT que el resto de la API. Solo lo llama el handler (no un browser), asi que no se abre
 * a otros origenes.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /** Mismo tope de codigo que los endpoints HTTP. */
    private static final int MAX_CODE_LENGTH = 200_000;

    private final ExecutionSocketHandler handler;

    public WebSocketConfig(
            LanguageService language,
            ObjectMapper json,
            @Value("${execution.input-timeout:5m}") Duration inputTimeout) {
        // Un hilo virtual por ejecucion: casi todo el tiempo esta esperando un input.
        this.handler =
                new ExecutionSocketHandler(
                        language,
                        json,
                        Executors.newVirtualThreadPerTaskExecutor(),
                        inputTimeout,
                        MAX_CODE_LENGTH);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/execute/stream");
    }
}
