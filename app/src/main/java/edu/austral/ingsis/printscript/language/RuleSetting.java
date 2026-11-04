package edu.austral.ingsis.printscript.language;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.austral.ingsis.printscript.analyzer.IdentifierCase;

/**
 * Una regla que pide el cliente: su nombre, si esta activa y un valor opcional (por ejemplo, la
 * cantidad de espacios de la indentacion). Las reglas que no vienen toman el valor por defecto de
 * PrintScript.
 */
public record RuleSetting(String name, boolean enabled, String value) {

    public static LintOptions toLintOptions(List<RuleSetting> rules) {
        Map<String, RuleSetting> byName = byName(rules);
        return new LintOptions(
                flag(byName, RuleCatalog.IDENTIFIER_CASE),
                identifierCase(byName),
                flag(byName, RuleCatalog.PRINTLN_ARGUMENT),
                flag(byName, RuleCatalog.READ_ARGUMENT));
    }

    public static FormatOptions toFormatOptions(List<RuleSetting> rules) {
        Map<String, RuleSetting> byName = byName(rules);
        return new FormatOptions(
                flag(byName, RuleCatalog.SPACE_BEFORE_COLON),
                flag(byName, RuleCatalog.SPACE_AFTER_COLON),
                flag(byName, RuleCatalog.SPACE_AROUND_EQUALS),
                number(byName, RuleCatalog.NEW_LINES_BEFORE_PRINTLN),
                number(byName, RuleCatalog.INDENT_SIZE),
                flag(byName, RuleCatalog.SPACE_AROUND_PARENS),
                flag(byName, RuleCatalog.IF_BRACE_BELOW_LINE));
    }

    private static Map<String, RuleSetting> byName(List<RuleSetting> rules) {
        Map<String, RuleSetting> byName = new HashMap<>();
        if (rules != null) {
            rules.forEach(rule -> byName.put(rule.name(), rule));
        }
        return byName;
    }

    private static Boolean flag(Map<String, RuleSetting> byName, String name) {
        RuleSetting rule = byName.get(name);
        return rule == null ? null : rule.enabled();
    }

    private static IdentifierCase identifierCase(Map<String, RuleSetting> byName) {
        RuleSetting rule = byName.get(RuleCatalog.IDENTIFIER_CASE);
        if (rule == null || !rule.enabled() || rule.value() == null) {
            return null;
        }
        try {
            return IdentifierCase.valueOf(rule.value().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    RuleCatalog.IDENTIFIER_CASE
                            + " admite CAMEL_CASE o SNAKE_CASE: "
                            + rule.value());
        }
    }

    private static Integer number(Map<String, RuleSetting> byName, String name) {
        RuleSetting rule = byName.get(name);
        if (rule == null || !rule.enabled() || rule.value() == null) {
            return null;
        }
        try {
            return Integer.parseInt(rule.value().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    name + " necesita un numero entero: " + rule.value());
        }
    }
}
