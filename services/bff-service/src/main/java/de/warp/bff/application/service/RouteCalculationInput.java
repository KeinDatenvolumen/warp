// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.bff.application.service;

import java.time.Instant;

public record RouteCalculationInput(
        RoutePoint start,
        RoutePoint destination,
        Instant departureTime,
        String activityProfile
) {

    public record RoutePoint(
            String label,
            double latitude,
            double longitude
    ) {
    }
}