// [KI-Log Nr. KI-162] (6e98a51) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-178] (503803e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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