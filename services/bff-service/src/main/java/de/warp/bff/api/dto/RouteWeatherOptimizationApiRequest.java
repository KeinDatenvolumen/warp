package de.warp.bff.api.dto;

import java.time.Instant;
import java.util.List;

public record RouteWeatherOptimizationApiRequest(
        String routeCalculationId,
        String activityProfile,
        Instant departureTime,
        Double durationSeconds,
        Double distanceMeters,
        List<List<Double>> coordinates,
        Instant windowStart,
        Instant windowEnd
) {
}
