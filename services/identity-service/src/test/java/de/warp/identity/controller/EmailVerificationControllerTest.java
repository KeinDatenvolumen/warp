package de.warp.identity.controller;

import de.warp.identity.exceptions.InvalidEmailVerificationTokenException;
import de.warp.identity.filter.CorrelationIdFilter;
import de.warp.identity.filter.InternalServiceAuthenticationFilter;
import de.warp.identity.service.EmailVerificationService;
import de.warp.identity.service.EmailVerificationResendService;
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
        controllers = EmailVerificationController.class,
        properties = {
                "warp.security.internal-service-token="
                        + "test-internal-service-token"
        }
)
class EmailVerificationControllerTest {

    private static final String CORRELATION_ID =
            "01J9ABCDEF1234567890";

    private static final String INTERNAL_SERVICE_TOKEN =
            "test-internal-service-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private EmailVerificationResendService resendService;

    @Test
    void shouldReturnNoContentForValidToken() throws Exception {
        String requestBody = """
                {
                  "token": "gueltiges-verifikationstoken"
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/confirm"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ));

        verify(emailVerificationService).confirmEmail(
                "gueltiges-verifikationstoken"
        );
    }

    @Test
    void shouldReturnBadRequestForInvalidToken() throws Exception {
        doThrow(new InvalidEmailVerificationTokenException())
                .when(emailVerificationService)
                .confirmEmail("ungueltiges-token");

        String requestBody = """
                {
                  "token": "ungueltiges-token"
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/confirm"
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
                        "/internal/v1/email-verifications/confirm"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(emailVerificationService).confirmEmail(
                "ungueltiges-token"
        );
    }

    @Test
    void shouldReturnBadRequestForMissingToken() throws Exception {
        doThrow(new InvalidEmailVerificationTokenException())
                .when(emailVerificationService)
                .confirmEmail(null);

        String requestBody = """
                {
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/confirm"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(
                        "INVALID_VERIFICATION_TOKEN"
                ));

        verify(emailVerificationService).confirmEmail(null);
    }

    @Test
    void shouldReturnBadRequestForBlankToken() throws Exception {
        doThrow(new InvalidEmailVerificationTokenException())
                .when(emailVerificationService)
                .confirmEmail("   ");

        String requestBody = """
                {
                  "token": "   "
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/confirm"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(
                        "INVALID_VERIFICATION_TOKEN"
                ));

        verify(emailVerificationService).confirmEmail("   ");
    }

    @Test
    void shouldRejectMalformedRequestBody() throws Exception {
        String malformedRequestBody = """
                {
                  "token":
                }
                """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/confirm"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedRequestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code").value(
                        "MALFORMED_REQUEST_BODY"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(emailVerificationService, never())
                .confirmEmail(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldReturnNoContentWhenResendingVerificationEmail()
            throws Exception {

        String requestBody = """
            {
              "email": "max@example.de"
            }
            """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/resend"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ));

        verify(resendService).resendVerificationEmail(
                "max@example.de"
        );
    }

    @Test
    void shouldReturnNoContentForMissingEmailWhenResending()
            throws Exception {

        String requestBody = """
            {
            }
            """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/resend"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER_NAME,
                        CORRELATION_ID
                ));

        verify(resendService).resendVerificationEmail(null);
    }

    @Test
    void shouldReturnNoContentForBlankEmailWhenResending()
            throws Exception {

        String requestBody = """
            {
              "email": "   "
            }
            """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/resend"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(resendService).resendVerificationEmail("   ");
    }

    @Test
    void shouldRejectMalformedResendRequestBody()
            throws Exception {

        String malformedRequestBody = """
            {
              "email":
            }
            """;

        mockMvc.perform(
                        post(
                                "/internal/v1/email-verifications/resend"
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedRequestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code").value(
                        "MALFORMED_REQUEST_BODY"
                ))
                .andExpect(jsonPath("$.correlationId").value(
                        CORRELATION_ID
                ));

        verify(resendService, never())
                .resendVerificationEmail(
                        org.mockito.ArgumentMatchers.any()
                );
    }
}