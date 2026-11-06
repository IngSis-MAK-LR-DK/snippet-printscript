package edu.austral.ingsis.printscript.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austral.ingsis.printscript.language.LanguageService;
import edu.austral.ingsis.printscript.security.SecurityConfig;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Prueba el contrato HTTP completo, con el LanguageService y la libreria PrintScript reales. */
@WebMvcTest(LanguageController.class)
@Import({SecurityConfig.class, LanguageService.class, RuleController.class})
class LanguageControllerTest {

    private static final String VALID_CODE = "let x: number = 5;\\nprintln(x);";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;

    // --- /validate --------------------------------------------------------------------------

    @Test
    void validateAcceptsValidCode() throws Exception {
        call("/validate", "{\"content\":\"" + VALID_CODE + "\",\"version\":\"1.1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void validateReportsInvalidCodeAsErrorsWithPosition() throws Exception {
        call("/validate", "{\"content\":\"let x: number = ;\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].rule").exists())
                .andExpect(jsonPath("$.errors[0].message").exists())
                .andExpect(jsonPath("$.errors[0].line").value(1))
                .andExpect(jsonPath("$.errors[0].column").isNumber());
    }

    @Test
    void versionDefaultsToTheLatest() throws Exception {
        call("/validate", "{\"content\":\"const x: number = 1;\"}")
                .andExpect(jsonPath("$.valid").value(true));
        call("/validate", "{\"content\":\"const x: number = 1;\",\"version\":\"1.0\"}")
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void unknownVersionIsBadRequest() throws Exception {
        call("/validate", "{\"content\":\"println(1);\",\"version\":\"9.9\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void missingContentIsBadRequest() throws Exception {
        call("/validate", "{\"version\":\"1.1\"}").andExpect(status().isBadRequest());
    }

    @Test
    void tooLargeContentIsBadRequest() throws Exception {
        String huge = "a".repeat(LanguageController.MAX_CODE_LENGTH + 1);

        call("/validate", "{\"content\":\"" + huge + "\"}").andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        call("/validate", "{no es json").andExpect(status().isBadRequest());
    }

    // --- /lint ------------------------------------------------------------------------------

    @Test
    void lintReturnsViolationsWithRuleAndPosition() throws Exception {
        call("/lint", "{\"content\":\"let my_var: number = 1;\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations[0].rule").value("identifier-case"))
                .andExpect(jsonPath("$.violations[0].message").exists())
                .andExpect(jsonPath("$.violations[0].line").value(1));
    }

    @Test
    void lintUsesTheRulesSentByTheClient() throws Exception {
        call(
                        "/lint",
                        "{\"content\":\"let my_var: number = 1;\","
                                + "\"rules\":[{\"name\":\"identifier-case\","
                                + "\"enabled\":true,\"value\":\"SNAKE_CASE\"}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations").isEmpty());
    }

    @Test
    void lintWithUnknownIdentifierCaseIsBadRequest() throws Exception {
        call(
                        "/lint",
                        "{\"content\":\"let x: number = 1;\","
                                + "\"rules\":[{\"name\":\"identifier-case\","
                                + "\"enabled\":true,\"value\":\"KEBAB\"}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void lintOfInvalidCodeIs422() throws Exception {
        call("/lint", "{\"content\":\"let = ;\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.error.start.line").value(1));
    }

    // --- /format ----------------------------------------------------------------------------

    @Test
    void formatReturnsFormattedContent() throws Exception {
        call("/format", "{\"content\":\"let   x:number=5;\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("let x: number = 5;\n"));
    }

    @Test
    void formatUsesTheRulesSentByTheClient() throws Exception {
        call(
                        "/format",
                        "{\"content\":\"let x: number = 5;\","
                                + "\"rules\":[{\"name\":\"space-around-equals\","
                                + "\"enabled\":false,\"value\":null}]}")
                .andExpect(jsonPath("$.content").value("let x: number=5;\n"));
    }

    @Test
    void formatWithInvalidRuleValueIsBadRequest() throws Exception {
        call(
                        "/format",
                        "{\"content\":\"println(1);\","
                                + "\"rules\":[{\"name\":\"new-lines-before-println\","
                                + "\"enabled\":true,\"value\":\"9\"}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void formatOfInvalidCodeIs422() throws Exception {
        call("/format", "{\"content\":\"println(;\"}").andExpect(status().isUnprocessableEntity());
    }

    // --- /execute ---------------------------------------------------------------------------

    @Test
    void executeReturnsOutputsAndNoErrors() throws Exception {
        call("/execute", "{\"content\":\"println(\\\"hola\\\");\\nprintln(1 + 2);\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputs[0]").value("hola"))
                .andExpect(jsonPath("$.outputs[1]").value("3"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void executeFeedsInputsAndEnvironment() throws Exception {
        String code =
                "let a: string = readInput(\\\"a\\\");\\n"
                        + "let b: string = readEnv(\\\"NOMBRE\\\");\\n"
                        + "println(a);\\nprintln(b);";

        call(
                        "/execute",
                        "{\"content\":\""
                                + code
                                + "\",\"inputs\":[\"uno\"],\"env\":{\"NOMBRE\":\"Ana\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputs[0]").value("uno"))
                .andExpect(jsonPath("$.outputs[1]").value("Ana"));
    }

    @Test
    void executeWithoutEnoughInputsReportsTheErrorWith200() throws Exception {
        call("/execute", "{\"content\":\"let a: string = readInput(\\\"nombre\\\");\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputs").isEmpty())
                .andExpect(jsonPath("$.errors[0]").value(Matchers.containsString("inputs")));
    }

    // --- /rules -----------------------------------------------------------------------------

    @Test
    void lintRulesAreListedWithTheirDefaults() throws Exception {
        mockMvc.perform(get("/rules").param("type", "LINT").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("identifier-case"))
                .andExpect(jsonPath("$[0].enabledByDefault").value(true))
                .andExpect(jsonPath("$[0].defaultValue").value("CAMEL_CASE"));
    }

    @Test
    void formatRulesAreListedWithTheirDefaults() throws Exception {
        mockMvc.perform(get("/rules").param("type", "FORMAT").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("space-before-colon"))
                .andExpect(jsonPath("$[0].enabledByDefault").value(false))
                .andExpect(jsonPath("$[3].defaultValue").value("0"));
    }

    @Test
    void unknownRuleTypeIsBadRequest() throws Exception {
        mockMvc.perform(get("/rules").param("type", "OTRO").with(asUser()))
                .andExpect(status().isBadRequest());
    }

    // --- seguridad --------------------------------------------------------------------------

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(
                        post("/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"println(1);\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions call(String path, String body) throws Exception {
        return mockMvc.perform(
                post(path).with(asUser()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static JwtRequestPostProcessor asUser() {
        return jwt().jwt(token -> token.subject("auth0|ana"));
    }
}
