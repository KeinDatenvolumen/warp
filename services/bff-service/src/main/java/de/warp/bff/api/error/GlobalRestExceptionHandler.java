package de.warp.bff.api.error;

import de.warp.bff.api.dto.ApiErrorResponse;
import de.warp.bff.api.dto.FieldErrorResponse;
import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.application.exception.TooManyLoginAttemptsException;
import de.warp.bff.application.exception.AccountChangeRejectedException;
import de.warp.bff.graphql.SessionInvalidException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import de.warp.bff.application.exception.RegistrationRejectedException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final String PROBLEM_BASE_URI =
            "https://warp.local/problems/";

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.UNAUTHORIZED,
                "invalid-credentials",
                "INVALID_CREDENTIALS",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(RegistrationRejectedException.class)
    public ResponseEntity<ApiErrorResponse>
    handleRegistrationRejected(
            RegistrationRejectedException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                exception.getStatus(),
                exception.getCode()
                        .toLowerCase(java.util.Locale.ROOT)
                        .replace('_', '-'),
                exception.getCode(),
                exception.getMessage(),
                exception.getFieldErrors(),
                request
        );
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyLoginAttempts(
            TooManyLoginAttemptsException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                URI.create(
                        PROBLEM_BASE_URI
                                + "too-many-login-attempts"
                ),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_LOGIN_ATTEMPTS",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(
                        HttpHeaders.RETRY_AFTER,
                        String.valueOf(
                                TooManyLoginAttemptsException
                                        .RETRY_AFTER_SECONDS
                        )
                )
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorResponse)
                .toList();

        boolean isRegistrationRequest =
                "/api/v1/users".equals(request.getRequestURI());

        String problemType = isRegistrationRequest
                ? "invalid-registration-request"
                : "invalid-session-request";

        String code = isRegistrationRequest
                ? "INVALID_REGISTRATION_REQUEST"
                : "INVALID_SESSION_REQUEST";

        String detail = isRegistrationRequest
                ? "Die Registrierungsdaten sind ungültig."
                : "Die Anmeldedaten sind ungültig.";

        return createProblemResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                problemType,
                code,
                detail,
                fieldErrors,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.BAD_REQUEST,
                "malformed-request-body",
                "MALFORMED_REQUEST_BODY",
                "Der Inhalt der Anfrage konnte nicht gelesen werden.",
                List.of(),
                request
        );
    }

    @ExceptionHandler(SessionInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalid(
            SessionInvalidException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.UNAUTHORIZED,
                "not-authenticated",
                "NOT_AUTHENTICATED",
                "Du bist nicht angemeldet.",
                List.of(),
                request
        );
    }

    @ExceptionHandler(AccountChangeRejectedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountChangeRejected(
            AccountChangeRejectedException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                exception.getStatus(),
                exception.getCode()
                        .toLowerCase(java.util.Locale.ROOT)
                        .replace('_', '-'),
                exception.getCode(),
                exception.getMessage(),
                exception.getFieldErrors(),
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> createProblemResponse(
            HttpStatus status,
            String problemType,
            String code,
            String detail,
            List<FieldErrorResponse> fieldErrors,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                URI.create(PROBLEM_BASE_URI + problemType),
                status.getReasonPhrase(),
                status.value(),
                code,
                detail,
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    private FieldErrorResponse toFieldErrorResponse(
            FieldError fieldError
    ) {
        return new FieldErrorResponse(
                fieldError.getField(),
                determineFieldErrorCode(fieldError),
                fieldError.getDefaultMessage()
        );
    }

    private String determineFieldErrorCode(
            FieldError fieldError
    ) {
        return switch (fieldError.getField()) {
            case "name" -> "INVALID_NAME";
            case "email" -> "INVALID_EMAIL";
            case "password" -> "INVALID_PASSWORD";
            default -> "INVALID_FIELD";
        };
    }

    @ExceptionHandler(DownstreamServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse>
    handleDownstreamServiceUnavailable(
            DownstreamServiceUnavailableException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "downstream-service-unavailable",
                "DOWNSTREAM_SERVICE_UNAVAILABLE",
                exception.getMessage(),
                List.of(),
                request
        );
    }
}