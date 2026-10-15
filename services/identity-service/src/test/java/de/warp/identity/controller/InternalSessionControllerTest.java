package de.warp.identity.controller;

import de.warp.identity.dto.CreateSessionRequest;
import de.warp.identity.dto.CreatedSessionResponse;
import de.warp.identity.exceptions.InvalidCredentialsException;
import de.warp.identity.filter.CorrelationIdFilter;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalSessionController.class)
class InternalSessionControllerTest {

    private static final String CORRELATION_ID =
            "01J9ABCDEF1234567890";

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

        when(sessionService.createSession(any(CreateSessionRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(post("/internal/v1/sessions")
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ))
                .andExpect(jsonPath("$.sessionToken")
                        .value("generated-session-token"))
                .andExpect(jsonPath("$.userId")
                        .value("usr_123"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-15T12:00:00Z"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-10-16T00:00:00Z"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        verify(sessionService)
                .createSession(any(CreateSessionRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(sessionService.createSession(any(CreateSessionRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        String requestBody = """
                {
                  "email": "unknown@example.de",
                  "password": "FalschesPasswort123"
                }
                """;

        mockMvc.perform(post("/internal/v1/sessions")
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ))
                .andExpect(jsonPath("$.type").value(
                        "https://warp.local/problems/invalid-credentials"
                ))
                .andExpect(jsonPath("$.title")
                        .value("Unauthorized"))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value(
                        "Die E-Mail-Adresse oder das Passwort ist nicht korrekt."
                ))
                .andExpect(jsonPath("$.instance")
                        .value("/internal/v1/sessions"))
                .andExpect(jsonPath("$.correlationId")
                        .value(CORRELATION_ID))
                .andExpect(jsonPath("$.sessionToken").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        verify(sessionService)
                .createSession(any(CreateSessionRequest.class));
    }

    @Test
    void shouldNormalizeEmailBeforeCallingService() throws Exception {
        CreatedSessionResponse response = new CreatedSessionResponse(
                "generated-session-token",
                "usr_123",
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-16T00:00:00Z")
        );

        when(sessionService.createSession(any(CreateSessionRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                  "email": "  Max@Example.de  ",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(post("/internal/v1/sessions")
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateSessionRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateSessionRequest.class);

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

        mockMvc.perform(post("/internal/v1/sessions")
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("email"))
                .andExpect(jsonPath("$.fieldErrors[0].code")
                        .value("INVALID_EMAIL"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(
                        "Die E-Mail-Adresse muss angegeben werden."
                ));

        verify(sessionService, never())
                .createSession(any(CreateSessionRequest.class));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        String malformedRequestBody = """
                {
                  "email": "max@example.de",
                  "password":
                }
                """;

        mockMvc.perform(post("/internal/v1/sessions")
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(
                        "MALFORMED_REQUEST_BODY"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(sessionService, never())
                .createSession(any(CreateSessionRequest.class));
    }
}