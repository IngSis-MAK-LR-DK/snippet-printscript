package edu.austral.ingsis.printscript.language;

/**
 * Un problema del codigo: la regla o el tipo de error que lo causo, el mensaje y la posicion (linea
 * y columna, desde 1). Es la misma forma para los errores de validacion y las violaciones de lint,
 * como pide el contrato con el Snippet Handler.
 */
public record Violation(String rule, String message, int line, int column) {

    static Violation of(String rule, String message, Location start) {
        return new Violation(rule, message, start.line(), start.column());
    }
}
