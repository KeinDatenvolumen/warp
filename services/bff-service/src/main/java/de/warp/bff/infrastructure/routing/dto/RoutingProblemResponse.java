// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.bff.infrastructure.routing.dto;

import java.util.List;

public record RoutingProblemResponse(
        String type,
        String title,
        Integer status,
        String code,
        String detail,
        String instance,
        String correlationId,
        List<FieldError> fieldErrors
) {

    public List<FieldError> safeFieldErrors() {
        return fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }

    public record FieldError(
            String field,
            String code,
            String message
    ) {
    }
}