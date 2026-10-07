package edu.austral.ingsis.printscript.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    /**
     * Valida firma, vencimiento, emisor (issuer) y destinatario (audience) del token. Es perezoso:
     * recien consulta a Auth0 cuando llega el primer token, asi el servicio arranca aunque Auth0 no
     * sea alcanzable en ese momento.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${auth0.audience}") String audience,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        return new SupplierJwtDecoder(() -> buildDecoder(audience, issuer));
    }

    private static JwtDecoder buildDecoder(String audience, String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        new AudienceValidator(audience));
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
