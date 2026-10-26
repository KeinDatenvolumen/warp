package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DepartureAlternativesCalculation(
        String calculationId,
        String routeCalculationId,
        Instant timeWindowStart,
        Instant timeWindowEnd,
        int intervalMinutes,
        Instant bestDepartureTime,
        List<DepartureAlternative> alternatives
) {

    public DepartureAlternativesCalculation {
        Objects.requireNonNull(calculationId, "calculationId must not be null");
        Objects.requireNonNull(routeCalculationId, "routeCalculationId must not be null");
        Objects.requireNonNull(timeWindowStart, "timeWindowStart must not be null");
        Objects.requireNonNull(timeWindowEnd, "timeWindowEnd must not be null");
        if (intervalMinutes <= 0) {
            throw new IllegalArgumentException("intervalMinutes must be positive");
        }
        Objects.requireNonNull(bestDepartureTime, "bestDepartureTime must not be null");
        alternatives = List.copyOf(Objects.requireNonNull(alternatives, "alternatives must not be null"));
    }
}
