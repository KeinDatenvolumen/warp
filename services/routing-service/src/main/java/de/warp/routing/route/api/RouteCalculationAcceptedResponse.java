// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.api;

import java.time.Instant;

import de.warp.routing.route.domain.RouteCalculationStatus;

public record RouteCalculationAcceptedResponse(
        String id,
        RouteCalculationStatus status,
        Instant createdAt
) {
}