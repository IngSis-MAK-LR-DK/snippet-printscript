package edu.austral.ingsis.printscript.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * La validacion del JWT (firma, vencimiento, issuer y audience) se configura en application.yml con
 * spring.security.oauth2.resourceserver.jwt.*, asi que no hace falta un JwtDecoder propio.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** /health es publico; todo lo demas exige un JWT valido. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                // El bearer token hace a la API stateless: sin sesion HTTP ni cookie de sesion.
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF depende de que el browser mande cookies solo; un bearer token no viaja asi.
                .csrf(AbstractHttpConfigurer::disable)
                // Solo lo llama el handler (no un browser de otro origen): CORS no hace falta.
                .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
