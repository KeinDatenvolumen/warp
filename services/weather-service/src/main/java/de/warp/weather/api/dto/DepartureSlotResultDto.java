// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.weather.api.dto;

import java.util.List;

public record DepartureSlotResultDto(
        String departureIso,
        String departureTime,
        String arrivalIso,
        String arrivalTime,
        int routeScore,
        double robustScore,
        boolean hasThunderCap,
        boolean reachesNightfall,
        List<Integer> segmentScores,
        List<String> segmentReasons,
        String summaryDeltaText
) {
}
