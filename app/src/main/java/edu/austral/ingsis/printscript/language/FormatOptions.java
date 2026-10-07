package edu.austral.ingsis.printscript.language;

import edu.austral.ingsis.printscript.formatter.FormatterConfig;

/** Reglas de formato; los campos en null toman el valor por defecto de PrintScript. */
public record FormatOptions(
        Boolean spaceBeforeColon,
        Boolean spaceAfterColon,
        Boolean spaceAroundEquals,
        Integer newLinesBeforePrintln,
        Integer indentSize,
        Boolean spaceAroundParens,
        Boolean ifBraceBelowLine) {

    /**
     * Lanza IllegalArgumentException si algun valor no es valido (por ejemplo, 3 saltos de linea).
     */
    FormatterConfig toConfig() {
        FormatterConfig defaults = FormatterConfig.defaultConfig();
        return new FormatterConfig(
                orElse(spaceBeforeColon, defaults.spaceBeforeColon()),
                orElse(spaceAfterColon, defaults.spaceAfterColon()),
                orElse(spaceAroundEquals, defaults.spaceAroundEquals()),
                orElse(newLinesBeforePrintln, defaults.newLinesBeforePrintln()),
                orElse(indentSize, defaults.indentSize()),
                orElse(spaceAroundParens, defaults.spaceAroundParens()),
                orElse(ifBraceBelowLine, defaults.ifBraceBelowLine()));
    }

    private static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
