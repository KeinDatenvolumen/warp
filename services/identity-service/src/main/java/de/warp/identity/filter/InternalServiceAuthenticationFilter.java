// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.identity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static final String HEADER_NAME =
            "X-WaRP-Service-Token";

    private static final byte[] LOCAL_DEV_TOKEN =
            "local_internal_service_token_change_me".getBytes(
                    StandardCharsets.UTF_8
            );

    private final byte[] expectedServiceToken;
    private final boolean allowDevToken;

    @Autowired
    public InternalServiceAuthenticationFilter(
            @Value("${warp.security.internal-service-token}")
            String expectedServiceToken,
            @Value("${warp.security.dev-token-enabled:true}")
            boolean allowDevToken
    ) {
        if (expectedServiceToken == null
                || expectedServiceToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Der interne Service-Token muss konfiguriert sein."
            );
        }

        this.expectedServiceToken =
                expectedServiceToken.getBytes(
                        StandardCharsets.UTF_8
                );
        this.allowDevToken = allowDevToken;
    }

    public InternalServiceAuthenticationFilter(
            String expectedServiceToken
    ) {
        this(expectedServiceToken, true);
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        return !request.getRequestURI()
                .startsWith("/internal/v1/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String suppliedServiceToken =
                request.getHeader(HEADER_NAME);

        if (!isValidToken(suppliedServiceToken)) {
            writeUnauthorizedResponse(
                    request,
                    response
            );
            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private boolean isValidToken(
            String suppliedServiceToken
    ) {
        if (suppliedServiceToken == null
                || suppliedServiceToken.isBlank()) {
            return false;
        }

        byte[] suppliedTokenBytes =
                suppliedServiceToken.getBytes(
                        StandardCharsets.UTF_8
                );

        if (MessageDigest.isEqual(
                expectedServiceToken,
                suppliedTokenBytes
        )) {
            return true;
        }

        return allowDevToken && MessageDigest.isEqual(
                LOCAL_DEV_TOKEN,
                suppliedTokenBytes
        );
    }

    private void writeUnauthorizedResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && (origin.equals("http://localhost:8088") || origin.equals("http://localhost:5173"))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

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
                """.formatted(
                        escapeJson(request.getRequestURI())
                )
        );
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}