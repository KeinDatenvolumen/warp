// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.client;

import de.warp.bff.api.dto.EmailVerificationConfirmRequest;
import de.warp.bff.api.dto.EmailVerificationResendRequest;
import de.warp.bff.api.exception.InvalidEmailVerificationTokenException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class IdentityEmailVerificationClient {

    private final RestClient identityRestClient;

    public IdentityEmailVerificationClient(
            @Qualifier("identityRestClient")
            RestClient identityRestClient
    ) {
        this.identityRestClient = Objects.requireNonNull(
                identityRestClient,
                "identityRestClient darf nicht null sein"
        );
    }

    public void confirmEmail(String token) {
        identityRestClient
                .post()
                .uri("/internal/v1/email-verifications/confirm")
                .body(new EmailVerificationConfirmRequest(token))
                .retrieve()
                .onStatus(
                        status -> status.value() == 400,
                        (request, response) -> {
                            throw new InvalidEmailVerificationTokenException();
                        }
                )
                .toBodilessEntity();
    }

    public void resendVerificationEmail(String email) {
        identityRestClient
                .post()
                .uri("/internal/v1/email-verifications/resend")
                .body(new EmailVerificationResendRequest(email))
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new IllegalStateException(
                                    "Der Identity-Service konnte "
                                            + "die Verifikations-E-Mail "
                                            + "nicht erneut versenden"
                            );
                        }
                )
                .toBodilessEntity();
    }
}