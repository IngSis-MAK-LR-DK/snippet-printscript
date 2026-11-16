package edu.austral.ingsis.printscript.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austral.ingsis.printscript.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VersionController.class)
@Import({SecurityConfig.class, VersionControllerTest.Build.class})
class VersionControllerTest {

    @TestConfiguration
    static class Build {
        @Bean
        BuildProperties buildProperties() {
            BuildProperties build = Mockito.mock(BuildProperties.class);
            when(build.get("printscript")).thenReturn("1.1.2");
            return build;
        }
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void theEngineVersionIsPublic() throws Exception {
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.1.2"));
    }
}
