// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.client;

import de.warp.bff.api.dto.PasswordResetConfirmRequest;
import de.warp.bff.api.dto.PasswordResetSendRequest;
import de.warp.bff.api.exception.InvalidPasswordResetTokenException;
import de.warp.bff.api.exception.PasswordRequirementsNotMetException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class IdentityPasswordResetClient {

    private final RestClient identityRestClient;

    public IdentityPasswordResetClient(
            @Qualifier("identityRestClient")
            RestClient identityRestClient
    ) {
        this.identityRestClient = Objects.requireNonNull(
                identityRestClient,
                "identityRestClient darf nicht null sein"
        );
    }

    public void sendPasswordResetEmail(String email) {
        identityRestClient
                .post()
                .uri("/internal/v1/password-resets/send")
                .body(
                        new PasswordResetSendRequest(email)
                )
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new IllegalStateException(
                                    "Der Identity-Service konnte "
                                            + "die Passwort-Rücksetzungs-E-Mail "
                                            + "nicht versenden"
                            );
                        }
                )
                .toBodilessEntity();
    }

    public void confirmPasswordReset(
            String token,
            String newPassword
    ) {
        identityRestClient
                .post()
                .uri("/internal/v1/password-resets/confirm")
                .body(
                        new PasswordResetConfirmRequest(
                                token,
                                newPassword
                        )
                )
                .retrieve()
                .onStatus(
                        status -> status.value() == 400,
                        (request, response) -> {
                            throw new InvalidPasswordResetTokenException();
                        }
                )
                .onStatus(
                        status -> status.value() == 422,
                        (request, response) -> {
                            throw new PasswordRequirementsNotMetException();
                        }
                )
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new IllegalStateException(
                                    "Der Identity-Service konnte "
                                            + "das Passwort nicht zurücksetzen"
                            );
                        }
                )
                .toBodilessEntity();
    }
}