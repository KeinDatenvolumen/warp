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
