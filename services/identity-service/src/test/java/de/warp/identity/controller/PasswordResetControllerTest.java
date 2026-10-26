package de.warp.identity.controller;

import de.warp.identity.exceptions.InvalidPasswordResetTokenException;
import de.warp.identity.exceptions.PasswordRequirementsNotMetException;
import de.warp.identity.filter.CorrelationIdFilter;
import de.warp.identity.filter.InternalServiceAuthenticationFilter;
import de.warp.identity.service.PasswordResetRequestService;
import de.warp.identity.service.PasswordResetService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PasswordResetController.class,
        properties = {
                "warp.security.internal-service-token="
                        + "test-internal-service-token"
        }
)
class PasswordResetControllerTest {

    private static final String CORRELATION_ID =
            "01J9ABCDEF1234567890";

    private static final String INTERNAL_SERVICE_TOKEN =
            "test-internal-service-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetRequestService requestService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void shouldReturnNoContentWhenSendingPasswordResetEmail()
            throws Exception {

        String requestBody = """
                {
                  "email": "max@example.de"
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/send"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ));

        verify(requestService).requestPasswordReset(
                "max@example.de"
        );
    }

    @Test
    void shouldReturnNoContentForMissingEmail()
            throws Exception {

        String requestBody = """
                {
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/send"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ));

        verify(requestService).requestPasswordReset(null);
    }

    @Test
    void shouldReturnNoContentForBlankEmail()
            throws Exception {

        String requestBody = """
                {
                  "email": "   "
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/send"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(requestService).requestPasswordReset("   ");
    }

    @Test
    void shouldRejectMalformedSendRequestBody()
            throws Exception {

        String malformedRequestBody = """
                {
                  "email":
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/send"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
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
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(requestService, never())
                .requestPasswordReset(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldReturnNoContentForValidPasswordReset()
            throws Exception {

        String requestBody = """
                {
                  "token": "gueltiges-reset-token",
                  "newPassword": "MeinNeuesPasswort123"
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/confirm"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ));

        verify(passwordResetService).resetPassword(
                "gueltiges-reset-token",
                "MeinNeuesPasswort123"
        );
    }

    @Test
    void shouldReturnBadRequestForInvalidResetToken()
            throws Exception {

        doThrow(new InvalidPasswordResetTokenException())
                .when(passwordResetService)
                .resetPassword(
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
                        post(
                                "/internal/v1/password-resets/confirm"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
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
                        "/internal/v1/password-resets/confirm"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(passwordResetService).resetPassword(
                "ungueltiges-reset-token",
                "MeinNeuesPasswort123"
        );
    }

    @Test
    void shouldReturnBadRequestForMissingResetToken()
            throws Exception {

        doThrow(new InvalidPasswordResetTokenException())
                .when(passwordResetService)
                .resetPassword(
                        null,
                        "MeinNeuesPasswort123"
                );

        String requestBody = """
                {
                  "newPassword": "MeinNeuesPasswort123"
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/confirm"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code").value(
                        "INVALID_RESET_TOKEN"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(passwordResetService).resetPassword(
                null,
                "MeinNeuesPasswort123"
        );
    }

    @Test
    void shouldReturnUnprocessableContentForShortPassword()
            throws Exception {

        doThrow(new PasswordRequirementsNotMetException())
                .when(passwordResetService)
                .resetPassword(
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
                        post(
                                "/internal/v1/password-resets/confirm"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
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
                        "/internal/v1/password-resets/confirm"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(passwordResetService).resetPassword(
                "gueltiges-reset-token",
                "zu-kurz"
        );
    }

    @Test
    void shouldReturnUnprocessableContentForMissingPassword()
            throws Exception {

        doThrow(new PasswordRequirementsNotMetException())
                .when(passwordResetService)
                .resetPassword(
                        "gueltiges-reset-token",
                        null
                );

        String requestBody = """
                {
                  "token": "gueltiges-reset-token"
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/confirm"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code").value(
                        "PASSWORD_REQUIREMENTS_NOT_MET"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(passwordResetService).resetPassword(
                "gueltiges-reset-token",
                null
        );
    }

    @Test
    void shouldRejectMalformedConfirmRequestBody()
            throws Exception {

        String malformedRequestBody = """
                {
                  "token": "gueltiges-reset-token",
                  "newPassword":
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/password-resets/confirm"
                        )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
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
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(passwordResetService, never())
                .resetPassword(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }
}