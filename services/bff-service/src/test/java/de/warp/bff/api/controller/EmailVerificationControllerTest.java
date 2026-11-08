// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-167] (af1ca24) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: add frontend email verification (WARP-007)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.api.controller;

import de.warp.bff.api.exception.InvalidEmailVerificationTokenException;
import de.warp.bff.application.service.SessionService;
import de.warp.bff.infrastructure.client.IdentityEmailVerificationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailVerificationController.class)
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityEmailVerificationClient identityClient;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void shouldConfirmEmailAndReturnNoContent() throws Exception {
        String requestBody = """
                {
                  "token": "gueltiges-verifikationstoken"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/email-verifications/confirm"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(identityClient).confirmEmail(
                "gueltiges-verifikationstoken"
        );
    }

    @Test
    void shouldResendEmailAndReturnNoContent() throws Exception {
        String requestBody = """
                {
                  "email": "max@example.de"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/email-verifications/resend"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(identityClient).resendVerificationEmail(
                "max@example.de"
        );
    }

    @Test
    void shouldForwardMissingConfirmToken() throws Exception {
        String requestBody = """
                {
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/email-verifications/confirm"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent());

        verify(identityClient).confirmEmail(null);
    }

    @Test
    void shouldForwardMissingResendEmail() throws Exception {
        String requestBody = """
                {
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/email-verifications/resend"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent());

        verify(identityClient).resendVerificationEmail(null);
    }

    @Test
    void shouldRejectMalformedConfirmRequest() throws Exception {
        String malformedRequestBody = """
                {
                  "token":
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/email-verifications/confirm"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedRequestBody)
                )
                .andExpect(status().isBadRequest());

        verify(identityClient, never()).confirmEmail(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldReturnInvalidVerificationTokenProblem()
            throws Exception {

        doThrow(new InvalidEmailVerificationTokenException())
                .when(identityClient)
                .confirmEmail("ungueltiges-token");

        String requestBody = """
            {
              "token": "ungueltiges-token"
            }
            """;

        mockMvc.perform(
                        post(
                                "/api/v1/email-verifications/confirm"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.type").value(
                        "https://warp.local/problems/"
                                + "invalid-verification-token"
                ))
                .andExpect(jsonPath("$.title").value(
                        "Bad Request"
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(
                        "INVALID_VERIFICATION_TOKEN"
                ))
                .andExpect(jsonPath("$.detail").value(
                        "Der E-Mail-Verifikationslink ist "
                                + "ungültig oder abgelaufen"
                ))
                .andExpect(jsonPath("$.instance").value(
                        "/api/v1/email-verifications/confirm"
                ))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        verify(identityClient).confirmEmail(
                "ungueltiges-token"
        );
    }
}