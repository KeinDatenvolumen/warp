package de.warp.bff.api.controller;

import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.application.service.CreatedSession;
import de.warp.bff.application.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

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
    }}

