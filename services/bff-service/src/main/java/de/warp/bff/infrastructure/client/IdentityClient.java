package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.infrastructure.client.dto.InternalCreateSessionRequest;
import de.warp.bff.infrastructure.client.dto.InternalCreatedSessionResponse;
import de.warp.bff.infrastructure.client.dto.InternalRegistrationRequest;
import de.warp.bff.infrastructure.client.dto.InternalRegistrationResponse;
import de.warp.bff.infrastructure.client.dto.InternalSessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class IdentityClient {

    private final RestClient restClient;
    private final String identityServiceUrl;

    public IdentityClient(
            RestClient restClient,
            @Value("${warp.services.identity.url}")
            String identityServiceUrl
    ) {
        this.restClient = restClient;
        this.identityServiceUrl =
                removeTrailingSlash(identityServiceUrl);
    }

    public InternalRegistrationResponse register(
            InternalRegistrationRequest request
    ) {
        InternalRegistrationResponse response = restClient
                .post()
                .uri(identityServiceUrl + "/internal/v1/users")
                .body(request)
                .retrieve()
                .body(InternalRegistrationResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Der Identity-Service lieferte keine Registrierungsantwort."
            );
        }

        return response;
    }

    public InternalCreatedSessionResponse createSession(
            InternalCreateSessionRequest request
    ) {
        try {
            InternalCreatedSessionResponse response = restClient
                    .post()
                    .uri(identityServiceUrl + "/internal/v1/sessions")
                    .body(request)
                    .retrieve()
                    .body(InternalCreatedSessionResponse.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Der Identity-Service lieferte keine Sitzung."
                );
            }

            return response;
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new InvalidCredentialsException();
        }
    }

    public InternalSessionResponse validateSession(
            String sessionToken
    ) {
        InternalSessionResponse response = restClient
                .get()
                .uri(
                        identityServiceUrl
                                + "/internal/v1/sessions/{sessionToken}",
                        sessionToken
                )
                .retrieve()
                .body(InternalSessionResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Der Identity-Service lieferte keine Sitzungsinformationen."
            );
        }

        return response;
    }

    public void invalidateSession(String sessionToken) {
        restClient
                .delete()
                .uri(
                        identityServiceUrl
                                + "/internal/v1/sessions/{sessionToken}",
                        sessionToken
                )
                .retrieve()
                .toBodilessEntity();
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}
