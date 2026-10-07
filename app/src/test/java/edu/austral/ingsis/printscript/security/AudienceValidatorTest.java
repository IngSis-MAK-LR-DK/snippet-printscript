package edu.austral.ingsis.printscript.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private static final String AUDIENCE = "https://snippet-searcher";

    private final AudienceValidator validator = new AudienceValidator(AUDIENCE);

    @Test
    void acceptsTokenIssuedForThisApi() {
        Jwt jwt = token(List.of("other", AUDIENCE));

        assertFalse(validator.validate(jwt).hasErrors());
    }

    @Test
    void rejectsTokenIssuedForAnotherApi() {
        Jwt jwt = token(List.of("https://other-api"));

        assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void rejectsTokenWithoutAudience() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").claim("sub", "auth0|ana").build();

        assertTrue(validator.validate(jwt).hasErrors());
    }

    private static Jwt token(List<String> audience) {
        return Jwt.withTokenValue("t").header("alg", "none").audience(audience).build();
    }
}
