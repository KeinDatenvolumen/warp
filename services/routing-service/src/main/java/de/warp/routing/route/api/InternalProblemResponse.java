// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.api;

import java.util.List;

public record InternalProblemResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String correlationId,
        List<FieldErrorResponse> fieldErrors
) {

    public record FieldErrorResponse(
            String field,
            String code,
            String message
    ) {
    }
}