package edu.austral.ingsis.printscript.language;

import edu.austral.ingsis.printscript.analyzer.AnalyzerConfig;
import edu.austral.ingsis.printscript.analyzer.IdentifierCase;

/** Reglas de linting; los campos en null toman el valor por defecto de PrintScript. */
public record LintOptions(
        Boolean identifierCaseCheckEnabled,
        IdentifierCase identifierCase,
        Boolean printlnArgumentMustBeIdentifierOrLiteral,
        Boolean readArgumentMustBeIdentifierOrLiteral) {

    AnalyzerConfig toConfig() {
        AnalyzerConfig defaults = AnalyzerConfig.defaultConfig();
        return new AnalyzerConfig(
                orElse(identifierCaseCheckEnabled, defaults.identifierCaseCheckEnabled()),
                orElse(identifierCase, defaults.identifierCase()),
                orElse(
                        printlnArgumentMustBeIdentifierOrLiteral,
                        defaults.printlnArgumentMustBeIdentifierOrLiteral()),
                orElse(
                        readArgumentMustBeIdentifierOrLiteral,
                        defaults.readArgumentMustBeIdentifierOrLiteral()));
    }

    private static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
