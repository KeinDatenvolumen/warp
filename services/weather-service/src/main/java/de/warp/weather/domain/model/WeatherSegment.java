package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WeatherSegment(
        int segmentIndex,
        Instant expectedArrivalTime,
        int score,
        String color,
        int distanceFromStartMeters,
        WeatherCondition condition,
        List<List<Double>> geometryCoordinates
) {

    public WeatherSegment {
        if (segmentIndex < 0) {
            throw new IllegalArgumentException("segmentIndex must not be negative");
        }
        Objects.requireNonNull(expectedArrivalTime, "expectedArrivalTime must not be null");
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        Objects.requireNonNull(color, "color must not be null");
        if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("color must be a valid hex color (#RRGGBB)");
        }
        if (distanceFromStartMeters < 0) {
            throw new IllegalArgumentException("distanceFromStartMeters must not be negative");
        }
        Objects.requireNonNull(condition, "condition must not be null");
        geometryCoordinates = geometryCoordinates == null
                ? List.of()
                : geometryCoordinates.stream()
                .map(List::copyOf)
                .toList();
    }
}
