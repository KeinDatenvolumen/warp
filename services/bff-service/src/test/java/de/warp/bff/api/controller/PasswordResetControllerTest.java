// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
package de.warp.bff.api.controller;

import de.warp.bff.api.exception.InvalidPasswordResetTokenException;
import de.warp.bff.api.exception.PasswordRequirementsNotMetException;
import de.warp.bff.application.service.SessionService;
import de.warp.bff.infrastructure.client.IdentityPasswordResetClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityPasswordResetClient identityClient;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void shouldSendPasswordResetEmailAndReturnNoContent()
            throws Exception {

        String requestBody = """
                {
                  "email": "max@example.de"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(identityClient).sendPasswordResetEmail(
                "max@example.de"
        );
    }

    @Test
    void shouldForwardMissingEmailAndReturnNoContent()
            throws Exception {

        String requestBody = """
                {
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(identityClient).sendPasswordResetEmail(null);
    }

    @Test
    void shouldRejectMalformedSendRequest()
            throws Exception {

        String malformedRequestBody = """
                {
                  "email":
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedRequestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(
                        "MALFORMED_REQUEST_BODY"
                ))
                .andExpect(jsonPath("$.instance").value(
                        "/api/v1/password-resets/send"
                ));

        verify(identityClient, never())
                .sendPasswordResetEmail(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldConfirmPasswordResetAndReturnNoContent()
            throws Exception {

        String requestBody = """
                {
                  "token": "gueltiges-reset-token",
                  "newPassword": "MeinNeuesPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(identityClient).confirmPasswordReset(
                "gueltiges-reset-token",
                "MeinNeuesPasswort123"
        );
    }

    @Test
    void shouldForwardMissingConfirmValues()
            throws Exception {

        String requestBody = """
                {
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(identityClient).confirmPasswordReset(
                null,
                null
        );
    }

    @Test
    void shouldRejectMalformedConfirmRequest()
            throws Exception {

        String malformedRequestBody = """
                {
                  "token": "gueltiges-reset-token",
                  "newPassword":
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedRequestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(
                        "MALFORMED_REQUEST_BODY"
                ))
                .andExpect(jsonPath("$.instance").value(
                        "/api/v1/password-resets/confirm"
                ));

        verify(identityClient, never())
                .confirmPasswordReset(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldReturnInvalidResetTokenProblem()
            throws Exception {

        doThrow(new InvalidPasswordResetTokenException())
                .when(identityClient)
                .confirmPasswordReset(
                        "ungueltiges-reset-token",
                        "MeinNeuesPasswort123"
                );

        String requestBody = """
                {
                  "token": "ungueltiges-reset-token",
                  "newPassword": "MeinNeuesPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.type").value(
                        "https://warp.local/problems/"
                                + "invalid-reset-token"
                ))
                .andExpect(jsonPath("$.title").value(
                        "Bad Request"
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(
                        "INVALID_RESET_TOKEN"
                ))
                .andExpect(jsonPath("$.detail").value(
                        "Der Passwort-Rücksetzungslink ist "
                                + "ungültig oder abgelaufen"
                ))
                .andExpect(jsonPath("$.instance").value(
                        "/api/v1/password-resets/confirm"
                ))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        verify(identityClient).confirmPasswordReset(
                "ungueltiges-reset-token",
                "MeinNeuesPasswort123"
        );
    }

    @Test
    void shouldReturnPasswordRequirementsProblem()
            throws Exception {

        doThrow(new PasswordRequirementsNotMetException())
                .when(identityClient)
                .confirmPasswordReset(
                        "gueltiges-reset-token",
                        "zu-kurz"
                );

        String requestBody = """
                {
                  "token": "gueltiges-reset-token",
                  "newPassword": "zu-kurz"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/password-resets/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.type").value(
                        "https://warp.local/problems/"
                                + "password-requirements-not-met"
                ))
                .andExpect(jsonPath("$.title").value(
                        "Unprocessable Content"
                ))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(
                        "PASSWORD_REQUIREMENTS_NOT_MET"
                ))
                .andExpect(jsonPath("$.detail").value(
                        "Das Passwort muss mindestens "
                                + "zwölf Zeichen lang sein"
                ))
                .andExpect(jsonPath("$.instance").value(
                        "/api/v1/password-resets/confirm"
                ))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        verify(identityClient).confirmPasswordReset(
                "gueltiges-reset-token",
                "zu-kurz"
        );
    }
}