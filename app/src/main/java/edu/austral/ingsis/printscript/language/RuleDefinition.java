package edu.austral.ingsis.printscript.language;

/** Una regla que ofrece el lenguaje, con su configuracion por defecto. */
public record RuleDefinition(
        String name, String description, boolean enabledByDefault, String defaultValue) {}
