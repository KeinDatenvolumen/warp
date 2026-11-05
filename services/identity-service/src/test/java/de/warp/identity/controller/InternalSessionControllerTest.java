// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.identity.controller;

import de.warp.identity.dto.CreateSessionRequest;
import de.warp.identity.dto.CreatedSessionResponse;
import de.warp.identity.dto.SessionResponse;
import de.warp.identity.exceptions.InvalidCredentialsException;
import de.warp.identity.exceptions.SessionExpiredException;
import de.warp.identity.exceptions.SessionNotFoundException;
import de.warp.identity.filter.CorrelationIdFilter;
import de.warp.identity.filter.InternalServiceAuthenticationFilter;
import de.warp.identity.service.SessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InternalSessionController.class,
        properties = {
                "warp.security.internal-service-token="
                        + "test-internal-service-token"
        }
)
class InternalSessionControllerTest {

    private static final String CORRELATION_ID =
            "01J9ABCDEF1234567890";

    private static final String INTERNAL_SERVICE_TOKEN =
            "test-internal-service-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void shouldReturnOkForValidCredentials() throws Exception {
        CreatedSessionResponse response = new CreatedSessionResponse(
                "generated-session-token",
                "usr_123",
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-16T00:00:00Z")
        );

        when(sessionService.createSession(
                any(CreateSessionRequest.class)
        )).thenReturn(response);

        String requestBody = """
                {
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/sessions")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.sessionToken")
                                .value("generated-session-token")
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value("usr_123")
                )
                .andExpect(
                        jsonPath("$.createdAt")
                                .value("2026-10-15T12:00:00Z")
                )
                .andExpect(
                        jsonPath("$.expiresAt")
                                .value("2026-10-16T00:00:00Z")
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.email")
                                .doesNotExist()
                );

        verify(sessionService)
                .createSession(
                        any(CreateSessionRequest.class)
                );
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials()
            throws Exception {
        when(sessionService.createSession(
                any(CreateSessionRequest.class)
        )).thenThrow(new InvalidCredentialsException());

        String requestBody = """
                {
                  "email": "unknown@example.de",
                  "password": "FalschesPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/sessions")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.type").value(
                                "https://warp.local/problems/"
                                        + "invalid-credentials"
                        )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_CREDENTIALS")
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Die E-Mail-Adresse oder das Passwort "
                                        + "ist nicht korrekt."
                        )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/internal/v1/sessions")
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                )
                .andExpect(
                        jsonPath("$.sessionToken")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.email")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.fieldErrors")
                                .doesNotExist()
                );

        verify(sessionService)
                .createSession(
                        any(CreateSessionRequest.class)
                );
    }

    @Test
    void shouldNormalizeEmailBeforeCallingService()
            throws Exception {
        CreatedSessionResponse response = new CreatedSessionResponse(
                "generated-session-token",
                "usr_123",
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-16T00:00:00Z")
        );

        when(sessionService.createSession(
                any(CreateSessionRequest.class)
        )).thenReturn(response);

        String requestBody = """
                {
                  "email": "  Max@Example.de  ",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/sessions")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk());

        ArgumentCaptor<CreateSessionRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        CreateSessionRequest.class
                );

        verify(sessionService)
                .createSession(requestCaptor.capture());

        CreateSessionRequest capturedRequest =
                requestCaptor.getValue();

        assertThat(capturedRequest.email())
                .isEqualTo("max@example.de");

        assertThat(capturedRequest.password())
                .isEqualTo("MeinSicheresPasswort123");
    }

    @Test
    void shouldRejectMissingEmail() throws Exception {
        String requestBody = """
                {
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/sessions")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(
                        status().isUnprocessableContent()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(422)
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].field")
                                .value("email")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].code")
                                .value("INVALID_EMAIL")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].message").value(
                                "Die E-Mail-Adresse muss "
                                        + "angegeben werden."
                        )
                );

        verify(sessionService, never())
                .createSession(
                        any(CreateSessionRequest.class)
                );
    }

    @Test
    void shouldReturnBadRequestForMalformedJson()
            throws Exception {
        String malformedRequestBody = """
                {
                  "email": "max@example.de",
                  "password":
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/sessions")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedRequestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("MALFORMED_REQUEST_BODY")
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                );

        verify(sessionService, never())
                .createSession(
                        any(CreateSessionRequest.class)
                );
    }

    @Test
    void shouldReturnSessionForValidToken()
            throws Exception {
        SessionResponse response = new SessionResponse(
                "ses_123",
                "usr_123",
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-16T00:00:00Z"),
                true
        );

        when(sessionService.validateSession(
                "valid-session-token"
        )).thenReturn(response);

        mockMvc.perform(
                        post("/internal/v1/sessions/validation")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "sessionToken": "valid-session-token"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value("ses_123")
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value("usr_123")
                )
                .andExpect(
                        jsonPath("$.createdAt")
                                .value("2026-10-15T12:00:00Z")
                )
                .andExpect(
                        jsonPath("$.expiresAt")
                                .value("2026-10-16T00:00:00Z")
                )
                .andExpect(
                        jsonPath("$.sessionToken")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.tokenHash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );

        verify(sessionService)
                .validateSession("valid-session-token");
    }

    @Test
    void shouldReturnNotFoundForUnknownSession()
            throws Exception {
        when(sessionService.validateSession(
                "unknown-session-token"
        )).thenThrow(new SessionNotFoundException());

        mockMvc.perform(
                        post("/internal/v1/sessions/validation")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "sessionToken": "unknown-session-token"
                                        }
                                        """
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.type").value(
                                "https://warp.local/problems/"
                                        + "session-not-found"
                        )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("SESSION_NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Die Sitzung wurde nicht gefunden."
                                )
                )
                .andExpect(
                        jsonPath("$.instance").value(
                                "/internal/v1/sessions/validation"
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                )
                .andExpect(
                        jsonPath("$.sessionToken")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.tokenHash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.fieldErrors")
                                .doesNotExist()
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "unknown-session-token"
                                        )
                                )
                        )
                );

        verify(sessionService)
                .validateSession("unknown-session-token");
    }

    @Test
    void shouldReturnGoneForExpiredSession()
            throws Exception {
        when(sessionService.validateSession(
                "expired-session-token"
        )).thenThrow(new SessionExpiredException());

        mockMvc.perform(
                        post("/internal/v1/sessions/validation")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "sessionToken": "expired-session-token"
                                        }
                                        """
                                )
                )
                .andExpect(status().isGone())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.type").value(
                                "https://warp.local/problems/"
                                        + "session-expired"
                        )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Gone")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(410)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("SESSION_EXPIRED")
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Die Sitzung ist abgelaufen "
                                        + "oder wurde invalidiert."
                        )
                )
                .andExpect(
                        jsonPath("$.instance").value(
                                "/internal/v1/sessions/validation"
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                )
                .andExpect(
                        jsonPath("$.sessionToken")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.tokenHash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.fieldErrors")
                                .doesNotExist()
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "expired-session-token"
                                        )
                                )
                        )
                );

        verify(sessionService)
                .validateSession("expired-session-token");
    }

    @Test
    void shouldReturnNoContentWhenInvalidatingSession()
            throws Exception {
        mockMvc.perform(
                        post("/internal/v1/sessions/invalidation")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "sessionToken": "valid-session-token"
                                        }
                                        """
                                )
                )
                .andExpect(status().isNoContent())
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        content().string("")
                );

        verify(sessionService)
                .invalidateSession("valid-session-token");
    }

    @Test
    void shouldReturnNoContentForUnknownSessionDuringLogout()
            throws Exception {
        mockMvc.perform(
                        post("/internal/v1/sessions/invalidation")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "sessionToken": "unknown-session-token"
                                        }
                                        """
                                )
                )
                .andExpect(status().isNoContent())
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        content().string("")
                );

        verify(sessionService)
                .invalidateSession(
                        "unknown-session-token"
                );
    }
}