package de.warp.bff.security;

import de.warp.bff.application.service.SessionService;
import de.warp.bff.application.service.ValidatedSession;
import de.warp.bff.graphql.SessionInvalidException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class SessionAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String SESSION_COOKIE_NAME =
            "session";

    private final SessionService sessionService;

    public SessionAuthenticationFilter(
            SessionService sessionService
    ) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
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
                            session.expiresAt()
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
        }

        filterChain.doFilter(request, response);
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
                        SESSION_COOKIE_NAME.equals(cookie.getName())
                )
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}