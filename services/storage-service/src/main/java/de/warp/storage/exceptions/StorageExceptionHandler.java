// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-068] (a7d6e5a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/WARP-005: implement user registration
// [KI-Log Nr. KI-071] (7a94bc0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-005)
// [KI-Log Nr. KI-075] (14fc7f9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: not found exception and health check url (WARP-011)
// [KI-Log Nr. KI-083] (f894059) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add internal session creation endpoint
// [KI-Log Nr. KI-086] (f458eed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add session validation and invalidation
// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.exceptions;

import de.warp.storage.dto.ApiErrorResponse;
import de.warp.storage.dto.FieldErrorResponse;
import de.warp.storage.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class StorageExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StorageExceptionHandler.class);

    private static final String PROBLEM_BASE_URI =
            "https://warp.local/problems/";

    @ExceptionHandler(SavedRouteNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            SavedRouteNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "saved-route-not-found",
                "SAVED_ROUTE_NOT_FOUND",
                exception.getMessage(),
                List.of(),
                request
        );
    }

    @ExceptionHandler(InvalidSavedRouteRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            InvalidSavedRouteRequestException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "invalid-saved-route-request",
                "INVALID_SAVED_ROUTE_REQUEST",
                "Die Anfrage zur gespeicherten Route ist ungültig.",
                exception.getFieldErrors(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "invalid-saved-route-request",
                "INVALID_SAVED_ROUTE_REQUEST",
                "Die Anfrage zur gespeicherten Route ist ungültig.",
                fieldErrors,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "malformed-request-body",
                "MALFORMED_REQUEST_BODY",
                "Der Inhalt der Anfrage konnte nicht gelesen werden.",
                List.of(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "malformed-request-parameter",
                "MALFORMED_REQUEST_PARAMETER",
                "Ein Anfrageparameter hat ein ungültiges Format.",
                List.of(),
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "method-not-allowed",
                "METHOD_NOT_ALLOWED",
                "Die HTTP-Methode wird nicht unterstützt.",
                List.of(),
                request
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "unsupported-media-type",
                "UNSUPPORTED_MEDIA_TYPE",
                "Der Inhaltstyp wird nicht unterstützt.",
                List.of(),
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "not-found",
                "RESOURCE_NOT_FOUND",
                "Die angeforderte Ressource wurde nicht gefunden",
                List.of(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error(
                "Unerwarteter Fehler bei der Verarbeitung der Anfrage",
                exception
        );

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-server-error",
                "INTERNAL_SERVER_ERROR",
                "Bei der Verarbeitung der Anfrage ist ein unerwarteter "
                        + "Fehler aufgetreten.",
                List.of(),
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> problem(
            HttpStatus status,
            String problemType,
            String code,
            String detail,
            List<FieldErrorResponse> fieldErrors,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                URI.create(PROBLEM_BASE_URI + problemType),
                status.getReasonPhrase(),
                status.value(),
                code,
                detail,
                request.getRequestURI(),
                correlationId(request),
                fieldErrors
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    private String correlationId(HttpServletRequest request) {
        Object value =
                request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);

        if (value instanceof String correlationId
                && !correlationId.isBlank()) {
            return correlationId;
        }

        return "unknown";
    }

    private FieldErrorResponse toFieldError(FieldError fieldError) {
        return new FieldErrorResponse(
                fieldError.getField(),
                fieldErrorCode(fieldError.getField()),
                fieldError.getDefaultMessage()
        );
    }

    private String fieldErrorCode(String field) {
        return switch (field) {
            case "name" -> "INVALID_NAME";
            case "routeSnapshot.activityProfileCode" ->
                    "INVALID_ACTIVITY_PROFILE";
            default -> "INVALID_FIELD";
        };
    }
}