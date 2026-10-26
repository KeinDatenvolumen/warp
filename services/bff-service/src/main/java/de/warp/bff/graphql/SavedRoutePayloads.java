package de.warp.bff.graphql;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GraphQL-Antworttypen und Eingaben für gespeicherte Routen.
 */
public final class SavedRoutePayloads {

    private SavedRoutePayloads() {
    }

    public record RoutePlacePayload(
            String label,
            double latitude,
            double longitude
    ) {
    }

    public record RouteGeometryPayload(
            String type,
            List<List<Double>> coordinates
    ) {
    }

    public record SegmentWeatherPayload(
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double thunderstormProbabilityPercent
    ) {
    }

    public record WeatherSegmentPayload(
            int segmentIndex,
            OffsetDateTime expectedArrivalTime,
            int score,
            String color,
            SegmentWeatherPayload weather,
            RouteGeometryPayload geometry
    ) {
    }

    public record SavedRouteSummaryPayload(
            String id,
            String name,
            RoutePlacePayload start,
            RoutePlacePayload destination,
            ActivityProfileGraphQlController.ActivityProfilePayload
            activityProfile,
            int weatherScore,
            boolean favorite,
            OffsetDateTime createdAt
    ) {
    }

    public record SavedRoutePayload(
            String id,
            String name,
            RoutePlacePayload start,
            RoutePlacePayload destination,
            ActivityProfileGraphQlController.ActivityProfilePayload
            activityProfile,
            OffsetDateTime departureTime,
            OffsetDateTime arrivalTime,
            int distanceMeters,
            int durationSeconds,
            RouteGeometryPayload geometry,
            int weatherScore,
            String weatherScoreVersion,
            boolean thunderstormCapApplied,
            List<WeatherSegmentPayload> weatherSegments,
            boolean favorite,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record PageInfoPayload(
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record SavedRoutePagePayload(
            List<SavedRouteSummaryPayload> items,
            PageInfoPayload pageInfo
    ) {
    }

    public record UserErrorPayload(
            String field,
            String code,
            String message
    ) {
    }

    public record SaveRouteInput(
            String name,
            String routeCalculationId,
            String weatherEvaluationId,
            Boolean favorite
    ) {
    }

    public record SaveRoutePayload(
            SavedRoutePayload route,
            List<UserErrorPayload> errors
    ) {
    }

    public record UpdateSavedRoutePayload(
            SavedRoutePayload route,
            List<UserErrorPayload> errors
    ) {
    }

    public record DeleteSavedRoutePayload(
            String deletedRouteId,
            List<UserErrorPayload> errors
    ) {
    }

    public record SavedRouteFilterInput(Boolean favorite) {
    }

    public record PageInput(Integer page, Integer size) {
    }

    public record UpdateSavedRouteInput(String name, Boolean favorite) {
    }
}