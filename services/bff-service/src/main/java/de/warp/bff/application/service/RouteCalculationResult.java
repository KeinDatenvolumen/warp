package de.warp.bff.application.service;

import java.time.Instant;
import java.util.List;

import tools.jackson.databind.JsonNode;

public record RouteCalculationResult(
        String id,
        String status,
        RoutePoint start,
        RoutePoint destination,
        ActivityProfileSnapshot activityProfile,
        Instant departureTime,
        Instant arrivalTime,
        Double distanceMeters,
        Double durationSeconds,
        JsonNode geometry,
        JsonNode segments,
        WeatherEvaluationSnapshot weatherEvaluation,
        Instant createdAt,
        RouteCalculationError error
) {

    public RouteCalculationResult(
            String id,
            String status,
            RoutePoint start,
            RoutePoint destination,
            ActivityProfileSnapshot activityProfile,
            Instant departureTime,
            Instant arrivalTime,
            Double distanceMeters,
            Double durationSeconds,
            JsonNode geometry,
            JsonNode segments,
            Instant createdAt,
            RouteCalculationError error
    ) {
        this(
                id,
                status,
                start,
                destination,
                activityProfile,
                departureTime,
                arrivalTime,
                distanceMeters,
                durationSeconds,
                geometry,
                segments,
                null,
                createdAt,
                error
        );
    }

    public RouteCalculationResult(
            String id,
            String status,
            RoutePoint start,
            RoutePoint destination,
            ActivityProfileSnapshot activityProfile,
            Instant departureTime,
            Instant arrivalTime,
            Double distanceMeters,
            Double durationSeconds,
            JsonNode geometry,
            Instant createdAt,
            RouteCalculationError error
    ) {
        this(
                id,
                status,
                start,
                destination,
                activityProfile,
                departureTime,
                arrivalTime,
                distanceMeters,
                durationSeconds,
                geometry,
                null,
                null,
                createdAt,
                error
        );
    }

    public record RoutePoint(
            String label,
            double latitude,
            double longitude
    ) {
    }

    public record ActivityProfileSnapshot(
            String code,
            String displayName
    ) {
    }

    public record RouteCalculationError(
            String code,
            String message
    ) {
    }

    public record WeatherEvaluationSnapshot(
            String evaluationId,
            String routeCalculationId,
            String status,
            Instant departureTime,
            int overallScore,
            String scoreVersion,
            boolean thunderstormCapApplied,
            Instant weatherDataGeneratedAt,
            Instant weatherDataExpiresAt,
            Instant createdAt,
            List<WeatherSegmentSnapshot> segments
    ) {
    }

    public record WeatherSegmentSnapshot(
            int segmentIndex,
            int distanceFromStartMeters,
            Instant expectedArrivalTime,
            int score,
            String color,
            WeatherConditionSnapshot weather,
            List<List<Double>> geometryCoordinates
    ) {
    }

    public record WeatherConditionSnapshot(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double thunderstormProbabilityPercent,
            Double temperatureCelsius,
            Integer weatherCode
    ) {
    }
}
