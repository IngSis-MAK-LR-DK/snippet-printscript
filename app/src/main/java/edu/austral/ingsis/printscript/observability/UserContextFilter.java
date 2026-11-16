package edu.austral.ingsis.printscript.observability;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Pone en el contexto de logging quien hace el request, asi cada linea que se loguea mientras se
 * atiende lo lleva sin que nadie lo escriba. Corre despues de validar el token. Solo va el {@code
 * sub} del token: nunca el token ni el email.
 */
public class UserContextFilter extends OncePerRequestFilter {

    public static final String USER = "user";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            MDC.put(USER, jwt.getToken().getSubject());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(USER);
        }
    }
}
