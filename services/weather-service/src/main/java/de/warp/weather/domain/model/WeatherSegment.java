// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
        List<List<Double>> geometryCoordinates,
        String limitingFactor,
        String reasonLabel
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

    public WeatherSegment(
            int segmentIndex,
            Instant expectedArrivalTime,
            int score,
            String color,
            int distanceFromStartMeters,
            WeatherCondition condition,
            List<List<Double>> geometryCoordinates
    ) {
        this(
                segmentIndex,
                expectedArrivalTime,
                score,
                color,
                distanceFromStartMeters,
                condition,
                geometryCoordinates,
                null,
                null
        );
    }
}
