package de.warp.bff.security;

import de.warp.bff.api.dto.ApiErrorResponse;
import de.warp.bff.application.exception
        .DownstreamServiceUnavailableException;
import de.warp.bff.application.service.SessionService;
import de.warp.bff.application.service.ValidatedSession;
import de.warp.bff.graphql.SessionInvalidException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class SessionAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String SESSION_COOKIE_NAME =
            "session";

    private static final String PROBLEM_TYPE =
            "https://warp.local/problems/"
                    + "downstream-service-unavailable";

    private static final String ERROR_CODE =
            "DOWNSTREAM_SERVICE_UNAVAILABLE";

    private static final String ERROR_MESSAGE =
            "Der Identitätsdienst ist momentan nicht verfügbar. "
                    + "Bitte versuche es später erneut.";

    private final SessionService sessionService;
    private final JsonMapper jsonMapper;

    public SessionAuthenticationFilter(
            SessionService sessionService,
            JsonMapper jsonMapper
    ) {
        this.sessionService = sessionService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String sessionToken = findSessionToken(request);

        if (sessionToken == null || sessionToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ValidatedSession session =
                    sessionService.validateSession(sessionToken);

            AuthenticatedSessionPrincipal principal =
                    new AuthenticatedSessionPrincipal(
                            session.userId(),
                            session.createdAt(),
                            session.expiresAt(),
                            session.emailVerified()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            null,
                            List.of()
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        } catch (SessionInvalidException exception) {
            SecurityContextHolder.clearContext();
        } catch (DownstreamServiceUnavailableException exception) {
            SecurityContextHolder.clearContext();
            writeServiceUnavailableResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeServiceUnavailableResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        ApiErrorResponse errorResponse =
                new ApiErrorResponse(
                        URI.create(PROBLEM_TYPE),
                        "Service Unavailable",
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        ERROR_CODE,
                        ERROR_MESSAGE,
                        request.getRequestURI(),
                        List.of()
                );

        response.setStatus(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE
        );
        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        jsonMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }

    private String findSessionToken(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie ->
                        SESSION_COOKIE_NAME.equals(
                                cookie.getName()
                        )
                )
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}