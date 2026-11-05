// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.bff.infrastructure.routing.dto;

import java.time.Instant;

public record InternalStartRouteCalculationRequest(
        String requestedByUserId,
        RoutePoint start,
        RoutePoint destination,
        Instant departureTime,
        ActivityProfileSnapshot activityProfile
) {

    public record RoutePoint(
            String label,
            double latitude,
            double longitude
    ) {
    }

    public record ActivityProfileSnapshot(
            String code,
            String routingProfile
    ) {
    }
}