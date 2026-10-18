package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.client.dto.*;
import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
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
                    .uri(
                            identityServiceUrl
                                    + "/internal/v1/sessions"
                    )
                    .body(request)
                    .retrieve()
                    .body(
                            InternalCreatedSessionResponse.class
                    );

            if (response == null) {
                throw new DownstreamServiceUnavailableException();
            }

            return response;
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new InvalidCredentialsException();
        } catch (HttpServerErrorException
                 | ResourceAccessException exception) {
            throw new DownstreamServiceUnavailableException(
                    exception
            );
        } catch (RestClientException exception) {
            throw new DownstreamServiceUnavailableException(
                    exception
            );
        }
    }

    public InternalSessionResponse validateSession(
            String sessionToken
    ) {
        try {
            InternalSessionResponse response = restClient
                    .post()
                    .uri(
                            identityServiceUrl
                                    + "/internal/v1/sessions/validation"
                    )
                    .body(
                            new InternalSessionTokenRequest(
                                    sessionToken
                            )
                    )
                    .retrieve()
                    .body(InternalSessionResponse.class);

            if (response == null) {
                throw new DownstreamServiceUnavailableException();
            }

            return response;
        } catch (HttpClientErrorException.NotFound
                 | HttpClientErrorException.Gone exception) {
            throw new SessionInvalidException(exception);
        } catch (RestClientException exception) {
            throw new DownstreamServiceUnavailableException(
                    exception
            );
        }
    }


    public void invalidateSession(String sessionToken) {
        try {
            restClient
                    .post()
                    .uri(
                            identityServiceUrl
                                    + "/internal/v1/sessions/invalidation"
                    )
                    .body(
                            new InternalSessionTokenRequest(
                                    sessionToken
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new DownstreamServiceUnavailableException(
                    exception
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
