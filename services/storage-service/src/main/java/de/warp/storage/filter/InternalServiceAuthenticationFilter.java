package de.warp.storage.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InternalServiceAuthenticationFilter
        extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-WaRP-Service-Token";

    private final byte[] expectedServiceToken;

    public InternalServiceAuthenticationFilter(
            @Value("${warp.security.internal-service-token}")
            String expectedServiceToken
    ) {
        if (expectedServiceToken == null
                || expectedServiceToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Der interne Service-Token muss konfiguriert sein."
            );
        }

        this.expectedServiceToken =
                expectedServiceToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String suppliedServiceToken = request.getHeader(HEADER_NAME);

        if (!isValidToken(suppliedServiceToken)) {
            writeUnauthorizedResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String suppliedServiceToken) {
        if (suppliedServiceToken == null
                || suppliedServiceToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedServiceToken,
                suppliedServiceToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeUnauthorizedResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write(
                """
                {
                  "type": "https://warp.local/problems/internal-service-unauthorized",
                  "title": "Unauthorized",
                  "status": 401,
                  "code": "INTERNAL_SERVICE_UNAUTHORIZED",
                  "detail": "Der interne Servicezugriff wurde verweigert.",
                  "instance": "%s",
                  "fieldErrors": []
                }
                """.formatted(escapeJson(request.getRequestURI()))
        );
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}