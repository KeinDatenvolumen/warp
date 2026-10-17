package de.warp.bff.api.controller;

import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.application.service.CreatedSession;
import de.warp.bff.application.service.SessionService;
import jakarta.servlet.http.Cookie;
import de.warp.bff.application.service.ValidatedSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import de.warp.bff.security.LoginRateLimiter;
import de.warp.bff.application.exception.TooManyLoginAttemptsException;
import de.warp.bff.application.exception.DownstreamServiceUnavailableException;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @Test
    void shouldCreateSessionAndSetSecureCookie() throws Exception {
        CreatedSession createdSession = new CreatedSession(
                "generated-session-token",
                "usr_123",
                Instant.parse("2026-10-17T12:00:00Z")
        );

        when(sessionService.createSession(any()))
                .thenReturn(createdSession);

        String requestBody = """
                {
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        "session=generated-session-token; Path=/; Secure; HttpOnly; SameSite=Lax"
                ))
                .andExpect(jsonPath("$.userId")
                        .value("usr_123"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-10-17T12:00:00Z"))
                .andExpect(jsonPath("$.sessionToken").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        verify(sessionService).createSession(any());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(sessionService.createSession(any()))
                .thenThrow(new InvalidCredentialsException());

        String requestBody = """
                {
                  "email": "max@example.de",
                  "password": "FalschesPasswort123"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(header().doesNotExist(
                        HttpHeaders.SET_COOKIE
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.sessionToken").doesNotExist());

        verify(sessionService).createSession(any());
    }

    @Test
    void shouldInvalidateSessionAndDeleteCookie() throws Exception {
        when(sessionService.validateSession(anyString()))
                .thenReturn(
                        new ValidatedSession(
                                "user-123",
                                Instant.parse("2026-10-17T08:00:00Z"),
                                Instant.parse("2026-10-17T20:00:00Z")
                        )
                );
        mockMvc.perform(delete("/api/v1/sessions/current")
                        .cookie(new Cookie(
                                "session",
                                "valid-session-token"
                        )))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("session=")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Path=/")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Secure")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("HttpOnly")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("SameSite=Lax")
                ));

        verify(sessionService)
                .invalidateSession("valid-session-token");
    }
    @Test
    void shouldReturnTooManyRequestsWhenLoginLimitIsExceeded()
            throws Exception {
        doThrow(new TooManyLoginAttemptsException())
                .when(loginRateLimiter)
                .checkLoginAllowed(
                        "max@example.de",
                        "127.0.0.1"
                );

        String requestBody = """
            {
              "email": "max@example.de",
              "password": "MeinSicheresPasswort123"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/sessions")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.RETRY_AFTER,
                                "60"
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("TOO_MANY_LOGIN_ATTEMPTS")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(429)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Es wurden zu viele "
                                                + "Anmeldeversuche durchgeführt. "
                                                + "Bitte versuche es später erneut."
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/api/v1/sessions")
                )
                .andExpect(
                        jsonPath("$.fieldErrors")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.sessionToken")
                                .doesNotExist()
                );

        verify(sessionService, never())
                .createSession(any());
    }
    @Test
    void shouldReturnServiceUnavailableWhenIdentityServiceFails()
            throws Exception {
        when(sessionService.createSession(any()))
                .thenThrow(
                        new DownstreamServiceUnavailableException(
                                new IllegalStateException(
                                        "Interne URL mit vertraulichen Details"
                                )
                        )
                );

        String requestBody = """
            {
              "email": "max@example.de",
              "password": "MeinSicheresPasswort123"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/sessions")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "DOWNSTREAM_SERVICE_UNAVAILABLE"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(503)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Der Identitätsdienst ist momentan "
                                                + "nicht verfügbar. Bitte "
                                                + "versuche es später erneut."
                                )
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.sessionToken")
                                .doesNotExist()
                );
    }
}
