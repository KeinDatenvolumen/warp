package de.warp.bff.api.error;

import de.warp.bff.api.dto.ApiErrorResponse;
import de.warp.bff.api.dto.FieldErrorResponse;
import de.warp.bff.application.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

        return createProblemResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "invalid-session-request",
                "INVALID_SESSION_REQUEST",
                "Die Anmeldedaten sind ungültig.",
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

    private String determineFieldErrorCode(FieldError fieldError) {
        return switch (fieldError.getField()) {
            case "email" -> "INVALID_EMAIL";
            case "password" -> "INVALID_PASSWORD";
            default -> "INVALID_FIELD";
        };
    }
}