package de.warp.bff.application.service;

import de.warp.bff.api.dto.RegistrationRequest;
import de.warp.bff.api.dto.RegistrationResponse;
import de.warp.bff.api.exception.DownstreamApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class RegistrationService {

    private final RestClient restClient;
    private final String identityServiceUrl;

    public RegistrationService(
            RestClient restClient,
            @Value("${warp.services.identity.url}")
            String identityServiceUrl
    ) {
        this.restClient = restClient;
        this.identityServiceUrl = removeTrailingSlash(
                identityServiceUrl
        );
    }

    public RegistrationResponse register(
            RegistrationRequest request
    ) {
        try {
            RegistrationResponse response = restClient
                    .post()
                    .uri(identityServiceUrl + "/internal/v1/users")
                    .body(request)
                    .retrieve()
                    .body(RegistrationResponse.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Identity-Service lieferte keine Registrierungsantwort."
                );
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new DownstreamApiException(
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString()
            );
        }
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}