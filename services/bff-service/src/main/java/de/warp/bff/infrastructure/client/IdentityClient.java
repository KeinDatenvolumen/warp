package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.client.dto.*;
import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import de.warp.bff.api.dto.FieldErrorResponse;
import de.warp.bff.application.exception.RegistrationRejectedException;
import de.warp.bff.infrastructure.identity.dto.IdentityProblemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@Component
public class IdentityClient {

    private final RestClient restClient;
    private final String identityServiceUrl;
    private final JsonMapper jsonMapper;

    public IdentityClient(
            @Qualifier("identityRestClient")
            RestClient restClient,
            JsonMapper jsonMapper,
            @Value("${warp.services.identity.url}")
            String identityServiceUrl
    ) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
        this.identityServiceUrl =
                removeTrailingSlash(identityServiceUrl);
    }

    public InternalRegistrationResponse register(
            InternalRegistrationRequest request
    ) {
        try {
            InternalRegistrationResponse response = restClient
                    .post()
                    .uri(
                            identityServiceUrl
                                    + "/internal/v1/users"
                    )
                    .body(request)
                    .retrieve()
                    .body(InternalRegistrationResponse.class);

            if (response == null) {
                throw new DownstreamServiceUnavailableException();
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw translateRegistrationError(exception);
        } catch (RestClientException exception) {
            throw new DownstreamServiceUnavailableException(
                    exception
            );
        }
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

    private RuntimeException translateRegistrationError(
            RestClientResponseException exception
    ) {
        IdentityProblemResponse problem =
                readIdentityProblem(exception);

        if (
                exception.getStatusCode().value() == 409
                        && "EMAIL_ALREADY_EXISTS".equals(
                        problem.code()
                )
        ) {
            return new RegistrationRejectedException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    "Für diese E-Mail-Adresse ist bereits "
                            + "ein Benutzerkonto vorhanden.",
                    sanitizeFieldErrors(
                            problem.safeFieldErrors()
                    )
            );
        }

        if (
                exception.getStatusCode().value() == 422
                        && "INVALID_PASSWORD".equals(
                        problem.code()
                )
        ) {
            return new RegistrationRejectedException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "INVALID_PASSWORD",
                    "Das Passwort erfüllt die Anforderungen nicht.",
                    List.of(
                            new FieldErrorResponse(
                                    "password",
                                    "INVALID_PASSWORD",
                                    "Das Passwort erfüllt die "
                                            + "Anforderungen nicht."
                            )
                    )
            );
        }

        if (
                exception.getStatusCode().value() == 422
                        && "INVALID_REGISTRATION_REQUEST".equals(
                        problem.code()
                )
        ) {
            return new RegistrationRejectedException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "INVALID_REGISTRATION_REQUEST",
                    "Die Registrierungsdaten sind ungültig.",
                    sanitizeFieldErrors(
                            problem.safeFieldErrors()
                    )
            );
        }

        return new DownstreamServiceUnavailableException(
                exception
        );
    }

    private IdentityProblemResponse readIdentityProblem(
            RestClientResponseException exception
    ) {
        try {
            IdentityProblemResponse problem =
                    jsonMapper.readValue(
                            exception.getResponseBodyAsByteArray(),
                            IdentityProblemResponse.class
                    );

            if (problem != null) {
                return problem;
            }
        } catch (RuntimeException ignored) {
            // Absichtlich leer:
            // Unlesbare Downstreamantwort wird nicht veröffentlicht.
        }

        return new IdentityProblemResponse(
                null,
                null,
                exception.getStatusCode().value(),
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private List<FieldErrorResponse> sanitizeFieldErrors(
            List<FieldErrorResponse> fieldErrors
    ) {
        return fieldErrors.stream()
                .filter(this::isSupportedRegistrationFieldError)
                .map(this::sanitizeFieldError)
                .toList();
    }

    private boolean isSupportedRegistrationFieldError(
            FieldErrorResponse fieldError
    ) {
        return fieldError != null
                && (
                "name".equals(fieldError.field())
                        || "email".equals(fieldError.field())
                        || "password".equals(fieldError.field())
        );
    }

    private FieldErrorResponse sanitizeFieldError(
            FieldErrorResponse fieldError
    ) {
        return switch (fieldError.field()) {
            case "name" -> new FieldErrorResponse(
                    "name",
                    "INVALID_NAME",
                    "Bitte gib deinen Namen ein."
            );
            case "email" -> new FieldErrorResponse(
                    "email",
                    "INVALID_EMAIL",
                    "Bitte gib eine gültige E-Mail-Adresse ein."
            );
            case "password" -> new FieldErrorResponse(
                    "password",
                    "INVALID_PASSWORD",
                    "Das Passwort erfüllt die Anforderungen nicht."
            );
            default -> throw new IllegalArgumentException(
                    "Nicht unterstütztes Registrierungsfeld."
            );
        };
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}
