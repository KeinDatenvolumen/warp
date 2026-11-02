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

    public record RouteWeatherOptimizationRequest(
            String requestedByUserId,
            RouteSnapshotRequest routeSnapshot,
            Instant windowStart,
            Instant windowEnd
    ) {
    }

    public record OptimalWindowSummaryResponse(
            String bestDeparture,
            String bestDepartureIso,
            int bestScore,
            String stableWindow,
            String advantageVsWorst,
            String worstDeparture,
            int worstScore
    ) {
    }

    public record DepartureSlotResponse(
            String departureIso,
            String departureTime,
            String arrivalIso,
            String arrivalTime,
            int routeScore,
            double robustScore,
            boolean hasThunderCap,
            boolean reachesNightfall,
            List<Integer> segmentScores,
            List<String> segmentReasons,
            String summaryDeltaText
    ) {
    }

    public record RouteWeatherOptimizationResponse(
            int recommendedSlotIndex,
            OptimalWindowSummaryResponse optimalWindowSummary,
            List<DepartureSlotResponse> departureSlots
    ) {
    }

    public record RouteWeatherAiAnalysisRequest(
            String requestedByUserId,
            String startLabel,
            String destinationLabel,
            RouteSnapshotRequest routeSnapshot
    ) {
    }

    public record HiddenParameterInsightResponse(
            String parameterKey,
            String parameterLabel,
            String category,
            String observedValue,
            String status,
            String interpretation
    ) {
    }

    public record CriticalSectionHazardResponse(
            int segmentIndexStart,
            int segmentIndexEnd,
            String distanceRangeKm,
            String timeWindow,
            int minSegmentScore,
            String severity,
            String primaryFactor,
            String title,
            String description,
            String recommendation
    ) {
    }

    public record RouteWeatherAiAnalysisResponse(
            String routeCalculationId,
            String activityProfileCode,
            String activityProfileDisplayName,
            Instant departureTime,
            int overallScore,
            boolean thunderstormCapApplied,
            String riskLevel,
            String headline,
            String overallSummary,
            String activityProfileAdvice,
            List<HiddenParameterInsightResponse> hiddenParameterInsights,
            List<CriticalSectionHazardResponse> criticalSections,
            List<String> recommendations,
            List<String> mcpToolsUsed,
            String modelName,
            Instant generatedAt
    ) {
    }
}
