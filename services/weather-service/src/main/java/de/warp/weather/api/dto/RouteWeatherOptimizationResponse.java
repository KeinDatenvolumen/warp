// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.weather.api.dto;

import java.util.List;

public record RouteWeatherOptimizationResponse(
        int recommendedSlotIndex,
        OptimalWindowSummaryDto optimalWindowSummary,
        List<DepartureSlotResultDto> departureSlots
) {
}
