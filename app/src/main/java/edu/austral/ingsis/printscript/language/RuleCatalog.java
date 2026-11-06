package edu.austral.ingsis.printscript.language;

import java.util.List;

import edu.austral.ingsis.printscript.analyzer.AnalyzerConfig;
import edu.austral.ingsis.printscript.formatter.FormatterConfig;

/**
 * Reglas que ofrece PrintScript. Los valores por defecto se leen de la libreria, asi el catalogo no
 * se desincroniza si la libreria cambia.
 */
public final class RuleCatalog {

    public static final String IDENTIFIER_CASE = "identifier-case";
    public static final String PRINTLN_ARGUMENT = "println-argument";
    public static final String READ_ARGUMENT = "read-argument";

    public static final String SPACE_BEFORE_COLON = "space-before-colon";
    public static final String SPACE_AFTER_COLON = "space-after-colon";
    public static final String SPACE_AROUND_EQUALS = "space-around-equals";
    public static final String NEW_LINES_BEFORE_PRINTLN = "new-lines-before-println";
    public static final String INDENT_SIZE = "indent-size";
    public static final String SPACE_AROUND_PARENS = "space-around-parens";
    public static final String IF_BRACE_BELOW_LINE = "if-brace-below-line";

    private RuleCatalog() {}

    public static List<RuleDefinition> lintRules() {
        AnalyzerConfig defaults = AnalyzerConfig.defaultConfig();
        return List.of(
                new RuleDefinition(
                        IDENTIFIER_CASE,
                        "Los identificadores siguen una convencion de nombres",
                        defaults.identifierCaseCheckEnabled(),
                        defaults.identifierCase().name()),
                new RuleDefinition(
                        PRINTLN_ARGUMENT,
                        "println solo recibe un identificador o un literal",
                        defaults.printlnArgumentMustBeIdentifierOrLiteral(),
                        null),
                new RuleDefinition(
                        READ_ARGUMENT,
                        "readInput y readEnv solo reciben un identificador o un literal",
                        defaults.readArgumentMustBeIdentifierOrLiteral(),
                        null));
    }

    public static List<RuleDefinition> formatRules() {
        FormatterConfig defaults = FormatterConfig.defaultConfig();
        return List.of(
                new RuleDefinition(
                        SPACE_BEFORE_COLON,
                        "Espacio antes de los dos puntos",
                        defaults.spaceBeforeColon(),
                        null),
                new RuleDefinition(
                        SPACE_AFTER_COLON,
                        "Espacio despues de los dos puntos",
                        defaults.spaceAfterColon(),
                        null),
                new RuleDefinition(
                        SPACE_AROUND_EQUALS,
                        "Espacios alrededor del igual",
                        defaults.spaceAroundEquals(),
                        null),
                new RuleDefinition(
                        NEW_LINES_BEFORE_PRINTLN,
                        "Saltos de linea antes de println (0, 1 o 2)",
                        true,
                        String.valueOf(defaults.newLinesBeforePrintln())),
                new RuleDefinition(
                        INDENT_SIZE,
                        "Espacios por nivel de indentacion",
                        true,
                        String.valueOf(defaults.indentSize())),
                new RuleDefinition(
                        SPACE_AROUND_PARENS,
                        "Espacios dentro de los parentesis",
                        defaults.spaceAroundParens(),
                        null),
                new RuleDefinition(
                        IF_BRACE_BELOW_LINE,
                        "La llave del if va en la linea siguiente",
                        defaults.ifBraceBelowLine(),
                        null));
    }
}
