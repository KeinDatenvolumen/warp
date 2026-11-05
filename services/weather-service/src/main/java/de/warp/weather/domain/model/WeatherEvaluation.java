// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WeatherEvaluation(
        String evaluationId,
        String routeCalculationId,
        String requestedByUserId,
        EvaluationStatus status,
        Instant departureTime,
        int overallScore,
        String scoreVersion,
        boolean thunderstormCapApplied,
        Instant weatherDataGeneratedAt,
        Instant weatherDataExpiresAt,
        Instant createdAt,
        List<WeatherSegment> segments
) {

    public WeatherEvaluation {
        Objects.requireNonNull(evaluationId, "evaluationId must not be null");
        if (evaluationId.isBlank()) {
            throw new IllegalArgumentException("evaluationId must not be blank");
        }
        Objects.requireNonNull(routeCalculationId, "routeCalculationId must not be null");
        if (routeCalculationId.isBlank()) {
            throw new IllegalArgumentException("routeCalculationId must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(departureTime, "departureTime must not be null");
        if (overallScore < 0 || overallScore > 100) {
            throw new IllegalArgumentException("overallScore must be between 0 and 100");
        }
        Objects.requireNonNull(scoreVersion, "scoreVersion must not be null");
        Objects.requireNonNull(weatherDataGeneratedAt, "weatherDataGeneratedAt must not be null");
        Objects.requireNonNull(weatherDataExpiresAt, "weatherDataExpiresAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        segments = List.copyOf(Objects.requireNonNull(segments, "segments must not be null"));
    }
}
