// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.weather.api.dto;

public record CriticalSectionHazardDto(
        int segmentIndexStart,
        int segmentIndexEnd,
        String distanceRangeKm,
        String timeWindow,
        int minSegmentScore,
        String severity,
        String primaryFactor,
        String title,
        String description,
        String recommendation
) {
}
