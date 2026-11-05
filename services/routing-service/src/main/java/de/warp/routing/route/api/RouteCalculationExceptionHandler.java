// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.warp.routing.route.application.RouteCalculationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(
        assignableTypes = RouteCalculationController.class
)
public class RouteCalculationExceptionHandler {

    private static final String CORRELATION_HEADER =
            "X-Correlation-Id";

    @ExceptionHandler(
            RouteCalculationNotFoundException.class
    )
    public ResponseEntity<InternalProblemResponse> handleNotFound(
            RouteCalculationNotFoundException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "ROUTE_CALCULATION_NOT_FOUND",
                "Die Routenberechnung wurde nicht gefunden.",
                request
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingRequestHeaderException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<InternalProblemResponse>
    handleInvalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "INVALID_ROUTE_REQUEST",
                "Die Routenanfrage ist ungültig.",
                request
        );
    }

    private ResponseEntity<InternalProblemResponse> createProblem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request
    ) {
        String correlationId =
                request.getHeader(CORRELATION_HEADER);

        if (correlationId == null
                || correlationId.isBlank()) {
            correlationId = UUID.randomUUID()
                    .toString();
        }

        String problemType =
                "https://warp.local/problems/"
                        + code.toLowerCase()
                        .replace("_", "-");

        InternalProblemResponse response =
                new InternalProblemResponse(
                        problemType,
                        status.getReasonPhrase(),
                        status.value(),
                        code,
                        detail,
                        request.getRequestURI(),
                        correlationId,
                        List.of()
                );

        return ResponseEntity.status(status)
                .contentType(
                        MediaType.APPLICATION_PROBLEM_JSON
                )
                .header(
                        CORRELATION_HEADER,
                        correlationId
                )
                .body(response);
    }
}