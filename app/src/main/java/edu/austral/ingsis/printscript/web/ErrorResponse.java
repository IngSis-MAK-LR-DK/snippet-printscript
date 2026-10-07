package edu.austral.ingsis.printscript.web;

import edu.austral.ingsis.printscript.language.SourceError;

import com.fasterxml.jackson.annotation.JsonInclude;

/** message siempre esta. error (con tipo, linea y columna) solo si el codigo no es valido. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, SourceError error) {

    static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }
}
