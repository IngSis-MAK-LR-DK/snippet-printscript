package edu.austral.ingsis.printscript.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import edu.austral.ingsis.printscript.analyzer.IdentifierCase;
import edu.austral.ingsis.printscript.common.PrintScriptException;
import edu.austral.ingsis.printscript.common.Version;

import org.junit.jupiter.api.Test;

class LanguageServiceTest {

    private static final Version V11 = Version.V1_1;

    private final LanguageService service = new LanguageService();

    // --- validate ---------------------------------------------------------------------------

    @Test
    void validProgramIsValid() {
        ValidationResult result = service.validate("let x: number = 5;\nprintln(x);", V11);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void syntaxErrorIsReportedWithPosition() {
        ValidationResult result = service.validate("let x: number = ;", V11);

        assertFalse(result.valid());
        assertEquals(1, result.errors().size());
        Violation error = result.errors().get(0);
        assertEquals(1, error.line());
        assertTrue(error.column() >= 1);
    }

    @Test
    void semanticErrorIsReportedWithItsType() {
        ValidationResult result = service.validate("println(undeclared);", V11);

        assertFalse(result.valid());
        assertEquals("SemanticException", result.errors().get(0).rule());
    }

    @Test
    void versionOnePointZeroRejectsOnePointOneFeatures() {
        ValidationResult result = service.validate("const x: number = 1;", Version.V1_0);

        assertFalse(result.valid());
    }

    // --- lint -------------------------------------------------------------------------------

    @Test
    void lintReportsIdentifierCaseViolationsWithTheRuleName() {
        List<Violation> violations = service.lint("let my_var: number = 1;", V11, null);

        assertFalse(violations.isEmpty());
        assertEquals(RuleCatalog.IDENTIFIER_CASE, violations.get(0).rule());
        assertEquals(1, violations.get(0).line());
    }

    @Test
    void lintWithSnakeCaseAcceptsSnakeCaseIdentifiers() {
        LintOptions snake = new LintOptions(true, IdentifierCase.SNAKE_CASE, null, null);

        assertTrue(service.lint("let my_var: number = 1;", V11, snake).isEmpty());
    }

    @Test
    void lintWithRuleDisabledReportsNothing() {
        LintOptions off = new LintOptions(false, null, false, false);

        assertTrue(service.lint("let my_var: number = 1;", V11, off).isEmpty());
    }

    @Test
    void lintOfInvalidCodeThrows() {
        assertThrows(PrintScriptException.class, () -> service.lint("let = ;", V11, null));
    }

    // --- format -----------------------------------------------------------------------------

    @Test
    void formatAppliesDefaultRules() {
        String formatted = service.format("let   x:number=5;", V11, null);

        assertEquals("let x: number = 5;\n", formatted);
    }

    @Test
    void formatAppliesGivenRules() {
        FormatOptions noEqualsSpace = new FormatOptions(null, null, false, null, null, null, null);

        assertEquals(
                "let x: number=5;\n", service.format("let x: number = 5;", V11, noEqualsSpace));
    }

    @Test
    void formatRejectsInvalidRuleValues() {
        FormatOptions tooManyLines = new FormatOptions(null, null, null, 3, null, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.format("println(1);", V11, tooManyLines));
    }

    // --- execute ----------------------------------------------------------------------------

    @Test
    void executeCollectsPrintlnOutputs() {
        ExecutionResult result =
                service.execute("println(\"hola\");\nprintln(1 + 2);", V11, null, null);

        assertEquals(List.of("hola", "3"), result.outputs());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void executeFeedsReadInputInOrder() {
        String code =
                "let a: string = readInput(\"a\");\n"
                        + "let b: string = readInput(\"b\");\n"
                        + "println(b);\nprintln(a);";

        ExecutionResult result = service.execute(code, V11, List.of("uno", "dos"), null);

        assertEquals(List.of("dos", "uno"), result.outputs());
    }

    @Test
    void executeReportsMissingInputAndKeepsThePartialOutput() {
        String code =
                "println(\"antes\");\n"
                        + "let a: string = readInput(\"nombre\");\n"
                        + "println(a);";

        ExecutionResult result = service.execute(code, V11, List.of(), null);

        assertEquals(List.of("antes"), result.outputs());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("no se enviaron mas inputs"));
    }

    @Test
    void executeReadsOnlyTheEnvironmentSentByTheClient() {
        String code = "let a: string = readEnv(\"NOMBRE\");\nprintln(a);";

        ExecutionResult result = service.execute(code, V11, null, Map.of("NOMBRE", "Ana"));

        assertEquals(List.of("Ana"), result.outputs());
    }

    @Test
    void executeOfInvalidCodeReportsTheErrorInsteadOfThrowing() {
        ExecutionResult result = service.execute("println(;", V11, null, null);

        assertTrue(result.outputs().isEmpty());
        assertEquals(1, result.errors().size());
    }
}
