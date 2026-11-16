package edu.austral.ingsis.printscript.execution;

import java.util.Map;

/** Hacia donde van los mensajes de una ejecucion: el WebSocket, o una lista en los tests. */
public interface MessageSink {

    /** Manda un mensaje; se serializa como un objeto JSON. */
    void send(Map<String, Object> message);

    /** Cierra la conexion: la ejecucion termino. */
    void close();
}
