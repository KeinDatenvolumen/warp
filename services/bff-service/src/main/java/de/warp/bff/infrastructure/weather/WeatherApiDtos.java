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
            String limitingFactor,
            String reasonLabel,
            WeatherConditionResponse weather,
            GeoJsonLineStringResponse geometry
    ) {
        public WeatherSegmentResponse(
                int segmentIndex,
                int distanceFromStartMeters,
                Instant expectedArrivalTime,
                int score,
                String color,
                WeatherConditionResponse weather,
                GeoJsonLineStringResponse geometry
        ) {
            this(
                    segmentIndex,
                    distanceFromStartMeters,
                    expectedArrivalTime,
                    score,
                    color,
                    null,
                    null,
                    weather,
                    geometry
            );
        }
    }

    public record WeatherConditionResponse(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double windGustsKmh,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double wetRoadIndex,
            Double snowfallCm,
            Double thunderstormProbabilityPercent,
            Double temperatureCelsius,
            Double apparentTemperatureCelsius,
            Double relativeHumidityPercent,
            Double dewPointCelsius,
            Double shortwaveRadiationWm2,
            Double soilTemperatureCelsius,
            Double visibilityMeters,
            Double capeJkg,
            Double uvIndex,
            Integer weatherCode
    ) {
        public WeatherConditionResponse(
                Double windSpeedKmh,
                Double windDirectionDegrees,
                Double precipitationProbabilityPercent,
                Double precipitationMm,
                Double thunderstormProbabilityPercent,
                Double temperatureCelsius,
                Integer weatherCode
        ) {
            this(
                    windSpeedKmh,
                    windDirectionDegrees,
                    null,
                    precipitationProbabilityPercent,
                    precipitationMm,
                    null,
                    null,
                    thunderstormProbabilityPercent,
                    temperatureCelsius,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    weatherCode
            );
        }
    }

    public record GeoJsonLineStringResponse(
            String type,
            List<List<Double>> coordinates
    ) {
    }
}
