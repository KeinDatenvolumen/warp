package de.warp.bff.infrastructure.client;

import de.warp.bff.api.dto.FieldErrorResponse;
import de.warp.bff.application.exception.AccountChangeRejectedException;
import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.client.dto.InternalChangeEmailRequest;
import de.warp.bff.infrastructure.client.dto.InternalChangePasswordRequest;
import de.warp.bff.infrastructure.client.dto.InternalUserResponse;
import de.warp.bff.infrastructure.identity.dto.IdentityProblemResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class IdentityUserClient {

    private static final Set<String> ALLOWED_FIELDS =
            Set.of("newEmail", "newPassword");

    private static final Map<String, String> FIELD_ERROR_MESSAGES = Map.of(
            "INVALID_EMAIL",
            "Bitte gib eine gültige E-Mail-Adresse ein.",
            "EMAIL_UNCHANGED",
            "Die neue E-Mail-Adresse muss sich von der aktuellen "
                    + "unterscheiden.",
            "PASSWORD_UNCHANGED",
            "Das neue Passwort muss sich vom aktuellen Passwort "
                    + "unterscheiden."
    );

    private static final String WRONG_PASSWORD_MESSAGE =
            "Das Passwort ist nicht korrekt.";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public IdentityUserClient(
            @Qualifier("identityRestClient") RestClient restClient,
            JsonMapper jsonMapper
    ) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    public InternalUserResponse getUser(String userId) {
        try {
            InternalUserResponse response = restClient
                    .get()
                    .uri("/internal/v1/users/{userId}", userId)
                    .retrieve()
                    .body(InternalUserResponse.class);

            if (response == null) {
                throw new DownstreamServiceUnavailableException();
            }

            return response;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new SessionInvalidException(exception);
        } catch (RestClientException exception) {
            throw new DownstreamServiceUnavailableException(exception);
        }
    }

    public void changePassword(
            String userId,
            String currentPassword,
            String newPassword
    ) {
        try {
            restClient
                    .post()
                    .uri(
                            "/internal/v1/users/{userId}/password-changes",
                            userId
                    )
                    .body(new InternalChangePasswordRequest(
                            currentPassword,
                            newPassword
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw translateError(exception, "currentPassword");
        } catch (RestClientException exception) {
            throw new DownstreamServiceUnavailableException(exception);
        }
    }

    public void changeEmail(
            String userId,
            String newEmail,
            String password
    ) {
        try {
            restClient
                    .post()
                    .uri(
                            "/internal/v1/users/{userId}/email-changes",
                            userId
                    )
                    .body(new InternalChangeEmailRequest(
                            newEmail,
                            password
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw translateError(exception, "password");
        } catch (RestClientException exception) {
            throw new DownstreamServiceUnavailableException(exception);
        }
    }

    private RuntimeException translateError(
            RestClientResponseException exception,
            String passwordField
    ) {
        int status = exception.getStatusCode().value();
        IdentityProblemResponse problem = readProblem(exception);
        String code = problem.code();

        if (status == 404) {
            return new SessionInvalidException(exception);
        }

        if (status == 403 && "INVALID_PASSWORD".equals(code)) {
            return new AccountChangeRejectedException(
                    HttpStatus.FORBIDDEN,
                    "INVALID_PASSWORD",
                    WRONG_PASSWORD_MESSAGE,
                    List.of(new FieldErrorResponse(
                            passwordField,
                            "INVALID_PASSWORD",
                            WRONG_PASSWORD_MESSAGE
                    ))
            );
        }

        if (status == 409 && "EMAIL_ALREADY_EXISTS".equals(code)) {
            String message = "Für diese E-Mail-Adresse existiert bereits "
                    + "ein Benutzerkonto.";

            return new AccountChangeRejectedException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    message,
                    List.of(new FieldErrorResponse(
                            "newEmail",
                            "EMAIL_ALREADY_EXISTS",
                            message
                    ))
            );
        }

        if (status == 422 && "PASSWORD_REQUIREMENTS_NOT_MET".equals(code)) {
            String message =
                    "Das Passwort muss mindestens zwölf Zeichen lang sein.";

            return new AccountChangeRejectedException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "PASSWORD_REQUIREMENTS_NOT_MET",
                    message,
                    List.of(new FieldErrorResponse(
                            "newPassword",
                            "PASSWORD_REQUIREMENTS_NOT_MET",
                            message
                    ))
            );
        }

        if (status == 422 && "INVALID_ACCOUNT_CHANGE_REQUEST".equals(code)) {
            return new AccountChangeRejectedException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "INVALID_ACCOUNT_CHANGE_REQUEST",
                    "Die Eingaben sind ungültig.",
                    sanitizeFieldErrors(problem.safeFieldErrors())
            );
        }

        return new DownstreamServiceUnavailableException(exception);
    }

    private List<FieldErrorResponse> sanitizeFieldErrors(
            List<FieldErrorResponse> fieldErrors
    ) {
        return fieldErrors.stream()
                .filter(error -> error != null
                        && ALLOWED_FIELDS.contains(error.field())
                        && FIELD_ERROR_MESSAGES.containsKey(error.code()))
                .map(error -> new FieldErrorResponse(
                        error.field(),
                        error.code(),
                        FIELD_ERROR_MESSAGES.get(error.code())
                ))
                .toList();
    }

    private IdentityProblemResponse readProblem(
            RestClientResponseException exception
    ) {
        try {
            IdentityProblemResponse problem = jsonMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    IdentityProblemResponse.class
            );

            if (problem != null) {
                return problem;
            }
        } catch (RuntimeException ignored) {
            // Unlesbare Downstream-Antwort wird nicht veröffentlicht.
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
}