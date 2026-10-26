package de.warp.storage.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Laut Vertrag setzt das BFF die Benutzer-ID im Header X-User-Id. Sie muss
 * zur Benutzer-ID im Pfad passen, sonst wird die Anfrage abgelehnt.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class UserContextFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-User-Id";

    private static final Pattern USER_PATH =
            Pattern.compile("^/internal/v1/users/([^/]+)(?:/.*)?$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !USER_PATH.matcher(request.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Matcher matcher = USER_PATH.matcher(request.getRequestURI());

        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerUserId = request.getHeader(HEADER_NAME);

        if (headerUserId == null || !headerUserId.equals(matcher.group(1))) {
            writeForbiddenResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeForbiddenResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Object correlationId =
                request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write(
                """
                {
                  "type": "https://warp.local/problems/user-context-mismatch",
                  "title": "Forbidden",
                  "status": 403,
                  "code": "USER_CONTEXT_MISMATCH",
                  "detail": "Der Benutzerkontext passt nicht zur Anfrage.",
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