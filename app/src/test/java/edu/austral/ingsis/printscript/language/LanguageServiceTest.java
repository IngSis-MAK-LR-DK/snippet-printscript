package edu.austral.ingsis.printscript.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import edu.austral.ingsis.printscript.analyzer.IdentifierCase;
import edu.austral.ingsis.printscript.common.PrintScriptException;
import edu.austral.ingsis.printscript.common.SemanticException;
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
        assertEquals(null, result.error());
    }

    @Test
    void syntaxErrorIsReportedWithPosition() {
        ValidationResult result = service.validate("let x: number = ;", V11);

        assertFalse(result.valid());
        assertNotNull(result.error());
        assertEquals(1, result.error().start().line());
        assertTrue(result.error().start().column() >= 1);
    }

    @Test
    void semanticErrorIsReported() {
        ValidationResult result = service.validate("println(undeclared);", V11);

        assertFalse(result.valid());
        assertEquals("SemanticException", result.error().type());
    }

    @Test
    void versionOnePointZeroRejectsOnePointOneFeatures() {
        ValidationResult result = service.validate("const x: number = 1;", Version.V1_0);

        assertFalse(result.valid());
    }

    // --- analyze ----------------------------------------------------------------------------

    @Test
    void analyzeReportsIdentifierCaseViolations() {
        List<Finding> findings = service.analyze("let my_var: number = 1;", V11, null);

        assertFalse(findings.isEmpty());
        assertEquals(1, findings.get(0).start().line());
    }

    @Test
    void analyzeWithSnakeCaseAcceptsSnakeCaseIdentifiers() {
        LintOptions snake = new LintOptions(true, IdentifierCase.SNAKE_CASE, null, null);

        assertTrue(service.analyze("let my_var: number = 1;", V11, snake).isEmpty());
    }

    @Test
    void analyzeWithRuleDisabledReportsNothing() {
        LintOptions off = new LintOptions(false, null, null, null);

        assertTrue(service.analyze("let my_var: number = 1;", V11, off).isEmpty());
    }

    @Test
    void analyzeOfInvalidCodeThrows() {
        assertThrows(PrintScriptException.class, () -> service.analyze("let = ;", V11, null));
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
        List<String> outputs =
                service.execute("println(\"hola\");\nprintln(1 + 2);", V11, null, null);

        assertEquals(List.of("hola", "3"), outputs);
    }

    @Test
    void executeFeedsReadInputInOrder() {
        String code =
                "let a: string = readInput(\"a\");\n"
                        + "let b: string = readInput(\"b\");\n"
                        + "println(b);\nprintln(a);";

        List<String> outputs = service.execute(code, V11, List.of("uno", "dos"), null);

        assertEquals(List.of("dos", "uno"), outputs);
    }

    @Test
    void executeFailsWithPositionWhenInputsRunOut() {
        String code = "let a: string = readInput(\"nombre\");\nprintln(a);";

        SemanticException error =
                assertThrows(
                        SemanticException.class, () -> service.execute(code, V11, List.of(), null));

        // La libreria envuelve el error del proveedor de inputs y le agrega la posicion del
        // readInput.
        assertTrue(error.getMessage().contains("no se enviaron mas inputs"));
        assertEquals(1, error.start().line());
    }

    @Test
    void executeReadsOnlyTheEnvironmentSentByTheClient() {
        String code = "let a: string = readEnv(\"NOMBRE\");\nprintln(a);";

        List<String> outputs = service.execute(code, V11, null, Map.of("NOMBRE", "Ana"));

        assertEquals(List.of("Ana"), outputs);
    }

    @Test
    void executeOfInvalidCodeThrows() {
        assertThrows(
                PrintScriptException.class, () -> service.execute("println(;", V11, null, null));
    }
}
