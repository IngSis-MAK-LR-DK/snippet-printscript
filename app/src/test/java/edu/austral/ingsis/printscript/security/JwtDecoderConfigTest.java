package edu.austral.ingsis.printscript.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtDecoderConfigTest {

    private static final String ISSUER = "https://tenant.example.com/";
    private static final String AUDIENCE = "https://snippet-searcher";

    private final OAuth2TokenValidator<Jwt> validator =
            JwtDecoderConfig.tokenValidator(AUDIENCE, ISSUER);

    @Test
    void buildsDecoderWithoutContactingTheIssuer() {
        // El issuer es inexistente: el decoder se resuelve de forma perezosa, al llegar un token.
        assertNotNull(new JwtDecoderConfig().jwtDecoder(AUDIENCE, "https://issuer.invalid/"));
    }

    @Test
    void acceptsAValidToken() {
        assertFalse(validator.validate(token(ISSUER, AUDIENCE, 3600)).hasErrors());
    }

    @Test
    void rejectsATokenFromAnotherIssuer() {
        assertTrue(
                validator.validate(token("https://otro.example.com/", AUDIENCE, 3600)).hasErrors());
    }

    @Test
    void rejectsATokenForAnotherApi() {
        assertTrue(validator.validate(token(ISSUER, "https://otra-api", 3600)).hasErrors());
    }

    @Test
    void rejectsAnExpiredToken() {
        assertTrue(validator.validate(token(ISSUER, AUDIENCE, -3600)).hasErrors());
    }

    /** expiresInSeconds negativo = ya vencido. */
    private static Jwt token(String issuer, String audience, long expiresInSeconds) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("t")
                .header("alg", "none")
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(now.minus(2, ChronoUnit.HOURS))
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .build();
    }
}
