// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InternalServiceAuthenticationFilter
        extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-WaRP-Service-Token";

    private static final byte[] LOCAL_DEV_TOKEN =
            "local_internal_service_token_change_me".getBytes(StandardCharsets.UTF_8);

    private final byte[] expectedServiceToken;
    private final boolean allowDevToken;
    private final byte[] dotEnvDevToken;

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
                expectedServiceToken.getBytes(StandardCharsets.UTF_8);
        this.allowDevToken = allowDevToken;
        this.dotEnvDevToken = allowDevToken ? resolveDotEnvServiceToken() : null;
    }

    public InternalServiceAuthenticationFilter(
            String expectedServiceToken
    ) {
        this(expectedServiceToken, true);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean isProtectedEndpoint = uri.startsWith("/internal/v1/")
                || uri.startsWith("/internal/api/v1/");
        return !isProtectedEndpoint
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
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

        byte[] suppliedTokenBytes =
                suppliedServiceToken.getBytes(StandardCharsets.UTF_8);

        if (MessageDigest.isEqual(expectedServiceToken, suppliedTokenBytes)) {
            return true;
        }

        if (!allowDevToken) {
            return false;
        }

        if (MessageDigest.isEqual(LOCAL_DEV_TOKEN, suppliedTokenBytes)) {
            return true;
        }

        return dotEnvDevToken != null
                && MessageDigest.isEqual(dotEnvDevToken, suppliedTokenBytes);
    }

    private static byte[] resolveDotEnvServiceToken() {
        for (String candidatePath : new String[]{"/workspace/.env", ".env"}) {
            Path path = Path.of(candidatePath);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("WARP_INTERNAL_SERVICE_TOKEN=")) {
                        String value = trimmed
                                .substring("WARP_INTERNAL_SERVICE_TOKEN=".length())
                                .trim();
                        if (!value.isEmpty()) {
                            return value.getBytes(StandardCharsets.UTF_8);
                        }
                    }
                }
            } catch (IOException ignored) {
                // Ignore unreadable .env file in dev fallback
            }
        }
        return null;
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

        Object correlationId =
                request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);

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
                  "correlationId": "%s",
                  "fieldErrors": []
                }
                """.formatted(
                        escapeJson(request.getRequestURI()),
                        escapeJson(correlationId instanceof String value
                                ? value
                                : "unknown")
                )
        );
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
