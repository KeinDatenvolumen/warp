package de.warp.identity.exceptions;

import de.warp.identity.dto.ApiErrorResponse;
import de.warp.identity.dto.FieldErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class RegistrationExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RegistrationExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        List<FieldErrorResponse> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorResponse)
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(
                "INVALID_REGISTRATION_REQUEST",
                "Die Registrierungsdaten sind ungültig.",
                fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                "MALFORMED_REQUEST_BODY",
                "Der Inhalt der Anfrage konnte nicht gelesen werden.",
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                "EMAIL_ALREADY_EXISTS",
                exception.getMessage(),
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        LOGGER.error("Unerwarteter Fehler bei der Verarbeitung der Anfrage", exception);

        ApiErrorResponse response = new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Bei der Verarbeitung der Anfrage ist ein unerwarteter Fehler aufgetreten.",
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
