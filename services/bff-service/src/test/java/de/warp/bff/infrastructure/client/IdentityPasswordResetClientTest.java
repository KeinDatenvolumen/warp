// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.client;

import de.warp.bff.api.exception.InvalidPasswordResetTokenException;
import de.warp.bff.api.exception.PasswordRequirementsNotMetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IdentityPasswordResetClientTest {

    private MockRestServiceServer mockServer;
    private IdentityPasswordResetClient identityClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder();

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        identityClient = new IdentityPasswordResetClient(
                restClientBuilder.build()
        );
    }

    @Test
    void shouldSendPasswordResetEmailThroughIdentityService() {
        mockServer.expect(
                        requestTo(
                                "/internal/v1/password-resets/send"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "email": "max@example.de"
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess()
                );

        identityClient.sendPasswordResetEmail(
                "max@example.de"
        );

        mockServer.verify();
    }

    @Test
    void shouldConfirmPasswordResetThroughIdentityService() {
        mockServer.expect(
                        requestTo(
                                "/internal/v1/password-resets/confirm"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "token": "gueltiges-reset-token",
                                  "newPassword": "MeinNeuesPasswort123"
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess()
                );

        identityClient.confirmPasswordReset(
                "gueltiges-reset-token",
                "MeinNeuesPasswort123"
        );

        mockServer.verify();
    }

    @Test
    void shouldTranslateInvalidResetToken() {
        mockServer.expect(
                        requestTo(
                                "/internal/v1/password-resets/confirm"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "token": "ungueltiges-reset-token",
                                  "newPassword": "MeinNeuesPasswort123"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .body(
                                        """
                                        {
                                          "status": 400,
                                          "code": "INVALID_RESET_TOKEN"
                                        }
                                        """
                                )
                );

        assertThatThrownBy(
                () -> identityClient.confirmPasswordReset(
                        "ungueltiges-reset-token",
                        "MeinNeuesPasswort123"
                )
        )
                .isInstanceOf(
                        InvalidPasswordResetTokenException.class
                )
                .hasMessage(
                        "Der Passwort-Rücksetzungslink ist "
                                + "ungültig oder abgelaufen"
                );

        mockServer.verify();
    }

    @Test
    void shouldTranslatePasswordRequirementsNotMet() {
        mockServer.expect(
                        requestTo(
                                "/internal/v1/password-resets/confirm"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "token": "gueltiges-reset-token",
                                  "newPassword": "zu-kurz"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(
                                HttpStatus.UNPROCESSABLE_CONTENT
                        )
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .body(
                                        """
                                        {
                                          "status": 422,
                                          "code": "PASSWORD_REQUIREMENTS_NOT_MET"
                                        }
                                        """
                                )
                );

        assertThatThrownBy(
                () -> identityClient.confirmPasswordReset(
                        "gueltiges-reset-token",
                        "zu-kurz"
                )
        )
                .isInstanceOf(
                        PasswordRequirementsNotMetException.class
                )
                .hasMessage(
                        "Das Passwort muss mindestens "
                                + "zwölf Zeichen lang sein"
                );

        mockServer.verify();
    }

    @Test
    void shouldRejectIdentityServerErrorWhenSendingEmail() {
        mockServer.expect(
                        requestTo(
                                "/internal/v1/password-resets/send"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        assertThatThrownBy(
                () -> identityClient.sendPasswordResetEmail(
                        "max@example.de"
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Der Identity-Service konnte "
                                + "die Passwort-Rücksetzungs-E-Mail "
                                + "nicht versenden"
                );

        mockServer.verify();
    }

    @Test
    void shouldRejectUnexpectedIdentityErrorWhenConfirming() {
        mockServer.expect(
                        requestTo(
                                "/internal/v1/password-resets/confirm"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        assertThatThrownBy(
                () -> identityClient.confirmPasswordReset(
                        "gueltiges-reset-token",
                        "MeinNeuesPasswort123"
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Der Identity-Service konnte "
                                + "das Passwort nicht zurücksetzen"
                );

        mockServer.verify();
    }

    @Test
    void shouldNotTreatUnauthorizedResponseAsInvalidResetToken() {
        mockServer.expect(
                        requestTo(
                                "/internal/v1/password-resets/confirm"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.UNAUTHORIZED)
                );

        assertThatThrownBy(
                () -> identityClient.confirmPasswordReset(
                        "gueltiges-reset-token",
                        "MeinNeuesPasswort123"
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Der Identity-Service konnte "
                                + "das Passwort nicht zurücksetzen"
                );

        mockServer.verify();
    }
}