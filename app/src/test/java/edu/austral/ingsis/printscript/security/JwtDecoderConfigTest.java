package edu.austral.ingsis.printscript.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class JwtDecoderConfigTest {

    @Test
    void buildsDecoderWithoutContactingTheIssuer() {
        // El issuer es inexistente: el decoder se resuelve de forma perezosa, al llegar un token.
        assertNotNull(new JwtDecoderConfig().jwtDecoder("aud", "https://issuer.invalid/"));
    }
}
