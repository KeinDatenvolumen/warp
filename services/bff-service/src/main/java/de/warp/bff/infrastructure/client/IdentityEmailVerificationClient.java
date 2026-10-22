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