package de.warp.bff.security;

import de.warp.bff.application.service.SessionService;
import de.warp.bff.application.service.ValidatedSession;
import de.warp.bff.graphql.SessionInvalidException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionAuthenticationFilterTest {

    private SessionService sessionService;
    private SessionAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        sessionService = mock(SessionService.class);
        filter = new SessionAuthenticationFilter(sessionService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotValidateSessionWithoutCookie() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();

        verify(sessionService, never())
                .validateSession(
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    @Test
    void shouldAuthenticateValidSession() throws Exception {
        String sessionToken = "valid-session-token";

        Instant createdAt =
                Instant.parse("2026-10-17T08:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-10-17T20:00:00Z");

        when(sessionService.validateSession(sessionToken))
                .thenReturn(
                        new ValidatedSession(
                                "user-123",
                                createdAt,
                                expiresAt
                        )
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie(
                        "session",
                        sessionToken
                )
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.isAuthenticated())
                .isTrue();

        assertThat(authentication.getPrincipal())
                .isInstanceOf(
                        AuthenticatedSessionPrincipal.class
                );

        AuthenticatedSessionPrincipal principal =
                (AuthenticatedSessionPrincipal)
                        authentication.getPrincipal();

        assertThat(principal.userId())
                .isEqualTo("user-123");

        assertThat(principal.createdAt())
                .isEqualTo(createdAt);

        assertThat(principal.expiresAt())
                .isEqualTo(expiresAt);

        verify(sessionService)
                .validateSession(sessionToken);
    }

    @Test
    void shouldRemainAnonymousForInvalidSession() throws Exception {
        String sessionToken = "invalid-session-token";

        when(sessionService.validateSession(sessionToken))
                .thenThrow(
                        new SessionInvalidException()
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie(
                        "session",
                        sessionToken
                )
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();

        verify(sessionService)
                .validateSession(sessionToken);
    }

    @Test
    void shouldIgnoreBlankSessionCookie() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie(
                        "session",
                        " "
                )
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();

        verify(sessionService, never())
                .validateSession(
                        org.mockito.ArgumentMatchers.anyString()
                );
    }
}