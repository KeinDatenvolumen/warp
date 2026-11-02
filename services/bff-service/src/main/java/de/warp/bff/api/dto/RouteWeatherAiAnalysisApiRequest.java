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
