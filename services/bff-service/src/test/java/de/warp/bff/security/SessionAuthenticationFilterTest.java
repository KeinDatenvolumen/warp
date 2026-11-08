// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-104] (2e8b6e1) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: improve SessionAuthenticationFilter and Tests (WARP-006)
// [KI-Log Nr. KI-200] (c059ad7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: require verified email for route calculation (WARP-030)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
import de.warp.bff.application.exception
        .DownstreamServiceUnavailableException;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

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
        filter = new SessionAuthenticationFilter(
                sessionService,
                JsonMapper.builder().build()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnServiceUnavailableWhenIdentityServiceFails()
            throws Exception {
        String sessionToken = "sensitive-session-token";

        when(sessionService.validateSession(sessionToken))
                .thenThrow(
                        new DownstreamServiceUnavailableException(
                                new IllegalStateException(
                                        "Interne technische Information"
                                )
                        )
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/graphql"
                );

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

        assertThat(response.getStatus())
                .isEqualTo(503);

        assertThat(response.getContentType())
                .startsWith(
                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                );

        assertThat(response.getContentAsString())
                .contains(
                        "\"code\":"
                                + "\"DOWNSTREAM_SERVICE_UNAVAILABLE\""
                )
                .contains(
                        "\"status\":503"
                )
                .contains(
                        "\"instance\":\"/graphql\""
                )
                .contains(
                        "Der Identitätsdienst ist momentan "
                                + "nicht verfügbar."
                )
                .doesNotContain(sessionToken)
                .doesNotContain(
                        "Interne technische Information"
                );

        assertThat(filterChain.getRequest())
                .isNull();

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();
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
                                expiresAt,
                                true
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

        assert principal != null;
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

        assertThat(response.getStatus())
                .isEqualTo(200);

        assertThat(filterChain.getRequest())
                .isSameAs(request);

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