// [KI-Log Nr. KI-162] (6e98a51) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-178] (503803e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-179] (5bc6d66) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
            String limitingFactor,
            String reasonLabel,
            WeatherConditionSnapshot weather,
            List<List<Double>> geometryCoordinates
    ) {
    }

    public record WeatherConditionSnapshot(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double windGustsKmh,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double wetRoadIndex,
            Double thunderstormProbabilityPercent,
            Double temperatureCelsius,
            Double apparentTemperatureCelsius,
            Integer weatherCode
    ) {
    }
}
