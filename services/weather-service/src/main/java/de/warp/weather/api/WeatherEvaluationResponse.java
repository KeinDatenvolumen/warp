package de.warp.weather.api;

import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherSegment;

import java.time.Instant;
import java.util.List;

public record WeatherEvaluationResponse(
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
        List<WeatherSegmentResponse> segments
) {

    public static WeatherEvaluationResponse from(WeatherEvaluation evaluation) {
        return new WeatherEvaluationResponse(
                evaluation.evaluationId(),
                evaluation.routeCalculationId(),
                evaluation.requestedByUserId(),
                evaluation.status(),
                evaluation.departureTime(),
                evaluation.overallScore(),
                evaluation.scoreVersion(),
                evaluation.thunderstormCapApplied(),
                evaluation.weatherDataGeneratedAt(),
                evaluation.weatherDataExpiresAt(),
                evaluation.createdAt(),
                evaluation.segments().stream()
                        .map(WeatherSegmentResponse::from)
                        .toList()
        );
    }

    public record WeatherSegmentResponse(
            int segmentIndex,
            int distanceFromStartMeters,
            Instant expectedArrivalTime,
            int score,
            String color,
            WeatherConditionResponse weather,
            GeoJsonLineStringResponse geometry
    ) {
        public static WeatherSegmentResponse from(WeatherSegment segment) {
            GeoJsonLineStringResponse geo = segment.geometryCoordinates() == null
                    || segment.geometryCoordinates().isEmpty()
                    ? null
                    : new GeoJsonLineStringResponse(
                    "LineString",
                    segment.geometryCoordinates()
            );

            return new WeatherSegmentResponse(
                    segment.segmentIndex(),
                    segment.distanceFromStartMeters(),
                    segment.expectedArrivalTime(),
                    segment.score(),
                    segment.color(),
                    WeatherConditionResponse.from(segment.condition()),
                    geo
            );
        }
    }

    public record WeatherConditionResponse(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double thunderstormProbabilityPercent,
            Double temperatureCelsius,
            Integer weatherCode
    ) {
        public static WeatherConditionResponse from(WeatherCondition condition) {
            if (condition == null) {
                return null;
            }
            return new WeatherConditionResponse(
                    condition.windSpeedKmh(),
                    condition.windDirectionDegrees(),
                    condition.precipitationProbabilityPercent(),
                    condition.precipitationMm(),
                    condition.thunderstormProbabilityPercent(),
                    condition.temperatureCelsius(),
                    condition.weatherCode()
            );
        }
    }

    public record GeoJsonLineStringResponse(
            String type,
            List<List<Double>> coordinates
    ) {
    }
}
