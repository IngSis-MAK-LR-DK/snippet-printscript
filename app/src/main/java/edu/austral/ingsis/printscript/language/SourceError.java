package edu.austral.ingsis.printscript.language;

import edu.austral.ingsis.printscript.common.PrintScriptException;

/** Un error del codigo: que paso, de que tipo y en que rango (start y end pueden ser null). */
public record SourceError(String type, String message, Location start, Location end) {

    public static SourceError of(PrintScriptException exception) {
        return new SourceError(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                Location.from(exception.start()),
                Location.from(exception.end()));
    }
}
