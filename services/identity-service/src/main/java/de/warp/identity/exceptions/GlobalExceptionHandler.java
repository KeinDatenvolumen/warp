package de.warp.identity.exceptions;

import de.warp.identity.dto.ApiErrorResponse;
import de.warp.identity.dto.FieldErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
}
