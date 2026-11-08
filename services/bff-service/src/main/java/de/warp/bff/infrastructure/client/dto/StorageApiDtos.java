// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.client.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Interne DTOs für die Kommunikation mit dem Storage-Service.
 */
public final class StorageApiDtos {

    private StorageApiDtos() {
    }

    public record Place(String label, double latitude, double longitude) {
    }

    public record GeoJsonLineString(
            String type,
            List<List<Double>> coordinates
    ) {
    }

    public record SegmentWeather(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double thunderstormProbabilityPercent
    ) {
    }

    public record WeatherSegment(
            int segmentIndex,
            OffsetDateTime expectedArrivalTime,
            int score,
            String color,
            SegmentWeather weather,
            GeoJsonLineString geometry
    ) {
    }

    public record RouteSnapshot(
            String routeCalculationId,
            Place start,
            Place destination,
            String activityProfileCode,
            OffsetDateTime departureTime,
            OffsetDateTime arrivalTime,
            int distanceMeters,
            int durationSeconds,
            GeoJsonLineString geometry
    ) {
    }

    public record WeatherSnapshot(
            String weatherEvaluationId,
            int overallScore,
            String scoreVersion,
            boolean thunderstormCapApplied,
            List<WeatherSegment> segments
    ) {
    }

    public record SavedRoute(
            String id,
            String name,
            boolean favorite,
            RouteSnapshot routeSnapshot,
            WeatherSnapshot weatherSnapshot,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record SavedRouteSummary(
            String id,
            String name,
            Place start,
            Place destination,
            String activityProfileCode,
            int weatherScore,
            boolean favorite,
            OffsetDateTime createdAt
    ) {
    }

    public record SavedRoutePage(
            List<SavedRouteSummary> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record CreateSavedRouteRequest(
            String name,
            boolean favorite,
            RouteSnapshot routeSnapshot,
            WeatherSnapshot weatherSnapshot
    ) {
    }

    public record UpdateSavedRouteRequest(String name, Boolean favorite) {
    }

    public record StorageFieldError(
            String field,
            String code,
            String message
    ) {
    }

    public record StorageProblem(
            String code,
            List<StorageFieldError> fieldErrors
    ) {
    }
}