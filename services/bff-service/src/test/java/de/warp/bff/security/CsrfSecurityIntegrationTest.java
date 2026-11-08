// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-150] (ceaba81) KI-generiert (Google Antigravity (Gemini 4 Argon Medium)): fix: change sameSite to None for the cookies to work with herokuapp (WARP-006)
// [KI-Log Nr. KI-200] (c059ad7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: require verified email for route calculation (WARP-030)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.security;

import de.warp.bff.application.service.CreatedSession;
import de.warp.bff.application.service.SessionService;
import de.warp.bff.application.service.ValidatedSession;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CsrfSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void shouldAllowLoginWithoutCsrfToken() throws Exception {
        when(sessionService.createSession(any()))
                .thenReturn(
                        new CreatedSession(
                                "generated-session-token",
                                "user-123",
                                Instant.parse(
                                        "2026-10-17T20:00:00Z"
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
                .andExpect(status().isOk());

        verify(sessionService)
                .createSession(any());
    }

    @Test
    void shouldRejectLogoutWithoutCsrfToken() throws Exception {
        String sessionToken = "valid-session-token";

        when(sessionService.validateSession(sessionToken))
                .thenReturn(
                        validSession()
                );

        mockMvc.perform(
                        delete("/api/v1/sessions/current")
                                .cookie(
                                        new Cookie(
                                                "session",
                                                sessionToken
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        verify(sessionService, never())
                .invalidateSession(sessionToken);
    }

    @Test
    void shouldAllowLogoutWithCsrfToken() throws Exception {
        String sessionToken = "valid-session-token";

        when(sessionService.validateSession(sessionToken))
                .thenReturn(
                        validSession()
                );

        mockMvc.perform(
                        delete("/api/v1/sessions/current")
                                .cookie(
                                        new Cookie(
                                                "session",
                                                sessionToken
                                        )
                                )
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        verify(sessionService)
                .invalidateSession(sessionToken);
    }

    @Test
    void shouldExposeCsrfToken() throws Exception {
        mockMvc.perform(
                        get("/api/v1/csrf")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.headerName")
                                .value("X-XSRF-TOKEN")
                )
                .andExpect(
                        jsonPath("$.parameterName")
                                .value("_csrf")
                )
                .andExpect(
                        jsonPath("$.token")
                                .isNotEmpty()
                )
                .andExpect(
                        cookie().exists("XSRF-TOKEN")
                )
                .andExpect(
                        cookie().sameSite("XSRF-TOKEN", "None")
                )
                .andExpect(
                        cookie().secure("XSRF-TOKEN", true)
                );
    }

    private ValidatedSession validSession() {
        return new ValidatedSession(
                "user-123",
                Instant.parse("2026-10-17T08:00:00Z"),
                Instant.parse("2026-10-17T20:00:00Z"),
                true
        );
    }
}