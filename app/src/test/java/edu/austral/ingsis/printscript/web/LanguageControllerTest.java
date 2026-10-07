package edu.austral.ingsis.printscript.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austral.ingsis.printscript.language.LanguageService;
import edu.austral.ingsis.printscript.security.SecurityConfig;

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
@Import({SecurityConfig.class, LanguageService.class})
class LanguageControllerTest {

    private static final String VALID_CODE = "let x: number = 5;\\nprintln(x);";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;

    // --- /validate --------------------------------------------------------------------------

    @Test
    void validateAcceptsValidCode() throws Exception {
        call("/validate", "{\"code\":\"" + VALID_CODE + "\",\"version\":\"1.1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void validateReportsInvalidCodeWithPositionAndStatus200() throws Exception {
        call("/validate", "{\"code\":\"let x: number = ;\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error.type").exists())
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.start.line").value(1))
                .andExpect(jsonPath("$.error.start.column").isNumber());
    }

    @Test
    void versionDefaultsToTheLatest() throws Exception {
        call("/validate", "{\"code\":\"const x: number = 1;\"}")
                .andExpect(jsonPath("$.valid").value(true));
        call("/validate", "{\"code\":\"const x: number = 1;\",\"version\":\"1.0\"}")
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void unknownVersionIsBadRequest() throws Exception {
        call("/validate", "{\"code\":\"println(1);\",\"version\":\"9.9\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void missingCodeIsBadRequest() throws Exception {
        call("/validate", "{\"version\":\"1.1\"}").andExpect(status().isBadRequest());
    }

    @Test
    void tooLargeCodeIsBadRequest() throws Exception {
        String huge = "a".repeat(LanguageController.MAX_CODE_LENGTH + 1);

        call("/validate", "{\"code\":\"" + huge + "\"}").andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        call("/validate", "{no es json").andExpect(status().isBadRequest());
    }

    // --- /analyze ---------------------------------------------------------------------------

    @Test
    void analyzeReturnsFindingsWithPosition() throws Exception {
        call("/analyze", "{\"code\":\"let my_var: number = 1;\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[0].message").exists())
                .andExpect(jsonPath("$.findings[0].start.line").value(1));
    }

    @Test
    void analyzeUsesTheGivenRules() throws Exception {
        call(
                        "/analyze",
                        "{\"code\":\"let my_var: number = 1;\","
                                + "\"config\":{\"identifierCase\":\"SNAKE_CASE\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings").isEmpty());
    }

    @Test
    void analyzeOfInvalidCodeIs422() throws Exception {
        call("/analyze", "{\"code\":\"let = ;\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.error.start.line").value(1));
    }

    // --- /format ----------------------------------------------------------------------------

    @Test
    void formatReturnsFormattedCode() throws Exception {
        call("/format", "{\"code\":\"let   x:number=5;\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formatted").value("let x: number = 5;\n"));
    }

    @Test
    void formatUsesTheGivenRules() throws Exception {
        call(
                        "/format",
                        "{\"code\":\"let x: number = 5;\",\"config\":{\"spaceAroundEquals\":false}}")
                .andExpect(jsonPath("$.formatted").value("let x: number=5;\n"));
    }

    @Test
    void formatWithInvalidRuleIsBadRequest() throws Exception {
        call("/format", "{\"code\":\"println(1);\",\"config\":{\"newLinesBeforePrintln\":9}}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void formatOfInvalidCodeIs422() throws Exception {
        call("/format", "{\"code\":\"println(;\"}").andExpect(status().isUnprocessableEntity());
    }

    // --- /execute ---------------------------------------------------------------------------

    @Test
    void executeReturnsOutputs() throws Exception {
        call("/execute", "{\"code\":\"println(\\\"hola\\\");\\nprintln(1 + 2);\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputs[0]").value("hola"))
                .andExpect(jsonPath("$.outputs[1]").value("3"));
    }

    @Test
    void executeFeedsInputsAndEnvironment() throws Exception {
        String code =
                "let a: string = readInput(\\\"a\\\");\\n"
                        + "let b: string = readEnv(\\\"NOMBRE\\\");\\n"
                        + "println(a);\\nprintln(b);";

        call(
                        "/execute",
                        "{\"code\":\""
                                + code
                                + "\",\"inputs\":[\"uno\"],\"env\":{\"NOMBRE\":\"Ana\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputs[0]").value("uno"))
                .andExpect(jsonPath("$.outputs[1]").value("Ana"));
    }

    @Test
    void executeWithoutEnoughInputsIs422WithPosition() throws Exception {
        call("/execute", "{\"code\":\"let a: string = readInput(\\\"nombre\\\");\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.start.line").value(1))
                .andExpect(
                        jsonPath("$.message")
                                .value(org.hamcrest.Matchers.containsString("inputs")));
    }

    // --- seguridad --------------------------------------------------------------------------

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(
                        post("/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"println(1);\"}"))
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
