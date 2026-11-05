// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.bff.api.dto;

import java.time.Instant;
import java.util.List;

public record RouteWeatherAiAnalysisApiRequest(
        String routeCalculationId,
        String startLabel,
        String destinationLabel,
        String activityProfile,
        Instant departureTime,
        Double durationSeconds,
        Double distanceMeters,
        List<List<Double>> coordinates
) {
}
