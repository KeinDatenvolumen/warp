package de.warp.weather.api.dto;

import java.util.List;

public record RouteWeatherOptimizationResponse(
        int recommendedSlotIndex,
        OptimalWindowSummaryDto optimalWindowSummary,
        List<DepartureSlotResultDto> departureSlots
) {
}
