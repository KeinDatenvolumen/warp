package de.warp.bff.api.exception;

import de.warp.bff.api.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice(
        basePackages = "de.warp.bff.api.controller"
)
public class RestApiExceptionHandler {

    private static final String PROBLEM_BASE_URI =
            "https://warp.local/problems/";

    @ExceptionHandler(
            InvalidEmailVerificationTokenException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleInvalidVerificationToken(
            InvalidEmailVerificationTokenException exception,
            HttpServletRequest request
    ) {

        return createProblemResponse(
                HttpStatus.BAD_REQUEST,
                "invalid-verification-token",
                "INVALID_VERIFICATION_TOKEN",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse>
    handleMalformedRequestBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {

        return createProblemResponse(
                HttpStatus.BAD_REQUEST,
                "malformed-request-body",
                "MALFORMED_REQUEST_BODY",
                "Der Inhalt der Anfrage konnte nicht gelesen werden.",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> createProblemResponse(
            HttpStatus status,
            String problemType,
            String code,
            String detail,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                URI.create(PROBLEM_BASE_URI + problemType),
                status.getReasonPhrase(),
                status.value(),
                code,
                detail,
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }
}