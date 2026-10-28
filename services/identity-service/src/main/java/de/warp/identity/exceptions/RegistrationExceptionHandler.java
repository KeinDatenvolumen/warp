package de.warp.identity.exceptions;

import de.warp.identity.dto.ApiErrorResponse;
import de.warp.identity.dto.FieldErrorResponse;
import de.warp.identity.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class RegistrationExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RegistrationExceptionHandler.class);

    private static final String PROBLEM_BASE_URI =
            "https://warp.local/problems/";

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.NOT_FOUND,
                "not-found",
                "RESOURCE_NOT_FOUND",
                "Die angeforderte Ressource wurde nicht gefunden",
                List.of(),
                request
        );
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

        String requestPath = request.getRequestURI();

        boolean isSessionRequest =
                requestPath.startsWith(
                        "/internal/v1/sessions"
                );
        String problemType = isSessionRequest
                ? "invalid-session-request"
                : "invalid-registration-request";

        String code = isSessionRequest
                ? "INVALID_SESSION_REQUEST"
                : "INVALID_REGISTRATION_REQUEST";

        String detail = isSessionRequest
                ? "Die Anmeldedaten sind ungültig."
                : "Die Registrierungsdaten sind ungültig.";

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
            HttpMessageNotReadableException exception,
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

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.CONFLICT,
                "email-already-exists",
                "EMAIL_ALREADY_EXISTS",
                exception.getMessage(),
                List.of(),
                request
        );
    }

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

    @ExceptionHandler(InvalidEmailVerificationTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidVerificationToken(
            InvalidEmailVerificationTokenException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.BAD_REQUEST,
                "invalid-verification-token",
                "INVALID_VERIFICATION_TOKEN",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.BAD_REQUEST,
                "invalid-reset-token",
                "INVALID_RESET_TOKEN",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(PasswordRequirementsNotMetException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordRequirementsNotMet(
            PasswordRequirementsNotMetException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "password-requirements-not-met",
                "PASSWORD_REQUIREMENTS_NOT_MET",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.NOT_FOUND,
                "user-not-found",
                "USER_NOT_FOUND",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleIncorrectPassword(
            IncorrectPasswordException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.FORBIDDEN,
                "invalid-password",
                "INVALID_PASSWORD",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(InvalidAccountChangeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAccountChange(
            InvalidAccountChangeException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "invalid-account-change-request",
                "INVALID_ACCOUNT_CHANGE_REQUEST",
                "Die Eingaben sind ungültig.",
                List.of(new FieldErrorResponse(
                        exception.getField(),
                        exception.getCode(),
                        exception.getMessage()
                )),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error(
                "Unerwarteter Fehler bei der Verarbeitung der Anfrage",
                exception
        );

        return createProblemResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-server-error",
                "INTERNAL_SERVER_ERROR",
                "Bei der Verarbeitung der Anfrage ist ein unerwarteter Fehler aufgetreten.",
                List.of(),
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
                getCorrelationId(request),
                fieldErrors
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    private String getCorrelationId(HttpServletRequest request) {
        Object correlationId =
                request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);

        if (correlationId instanceof String value && !value.isBlank()) {
            return value;
        }

        return "unknown";
    }

    private FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new FieldErrorResponse(
                fieldError.getField(),
                determineFieldErrorCode(fieldError),
                fieldError.getDefaultMessage()
        );
    }

    private String determineFieldErrorCode(FieldError fieldError) {
        return switch (fieldError.getField()) {
            case "password" -> "INVALID_PASSWORD";
            case "email" -> "INVALID_EMAIL";
            case "name" -> "INVALID_NAME";
            default -> "INVALID_FIELD";
        };
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotFound(
            SessionNotFoundException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.NOT_FOUND,
                "session-not-found",
                "SESSION_NOT_FOUND",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionExpired(
            SessionExpiredException exception,
            HttpServletRequest request
    ) {
        return createProblemResponse(
                HttpStatus.GONE,
                "session-expired",
                "SESSION_EXPIRED",
                exception.getMessage(),
                List.of(),
                request
        );
    }
}