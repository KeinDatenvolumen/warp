// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.weather.api.dto;

public record OptimalWindowSummaryDto(
        String bestDeparture,
        String bestDepartureIso,
        int bestScore,
        String stableWindow,
        String advantageVsWorst,
        String worstDeparture,
        int worstScore
) {
}
