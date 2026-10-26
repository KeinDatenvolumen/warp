package de.warp.bff.infrastructure.weather;

import java.time.Instant;
import java.util.List;

public final class WeatherApiDtos {

    private WeatherApiDtos() {
    }

    public record EvaluateRouteWeatherRequest(
            String requestedByUserId,
            RouteSnapshotRequest routeSnapshot
    ) {
    }

    public record RouteSnapshotRequest(
            String routeCalculationId,
            Instant departureTime,
            int durationSeconds,
            int distanceMeters,
            List<List<Double>> coordinates,
            ActivityProfileSnapshotRequest activityProfile
    ) {
    }

    public record ActivityProfileSnapshotRequest(
            String code,
            double averageSpeedKmh,
            int weatherQueryIntervalKm
    ) {
    }

    public record WeatherEvaluationResponse(
            String evaluationId,
            String routeCalculationId,
            String requestedByUserId,
            String status,
            Instant departureTime,
            int overallScore,
            String scoreVersion,
            boolean thunderstormCapApplied,
            Instant weatherDataGeneratedAt,
            Instant weatherDataExpiresAt,
            Instant createdAt,
            List<WeatherSegmentResponse> segments
    ) {
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
    }

    public record GeoJsonLineStringResponse(
            String type,
            List<List<Double>> coordinates
    ) {
    }
}
