// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.api;

import java.time.Instant;

public record StartRouteCalculationRequest(
        String requestedByUserId,
        RoutePointRequest start,
        RoutePointRequest destination,
        Instant departureTime,
        ActivityProfileSnapshotRequest activityProfile
) {
}