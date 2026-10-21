package de.warp.routing.route.api;

import java.time.Instant;

import de.warp.routing.profile.domain.ActivityProfileCode;
import de.warp.routing.route.domain.RouteCalculationStatus;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public record RouteCalculationResponse(
        String id,
        RouteCalculationStatus status,
        RoutePointResponse start,
        RoutePointResponse destination,
        ActivityProfileResponse activityProfile,
        Instant departureTime,
        Instant arrivalTime,
        Double distanceMeters,
        Double durationSeconds,
        JsonNode geometry,
        JsonNode segments,
        Instant createdAt,
        Instant updatedAt,
        RouteCalculationErrorResponse error
) {

    public static RouteCalculationResponse from(
            RouteCalculationEntity calculation,
            ObjectMapper objectMapper
    ) {
        RouteCalculationErrorResponse error =
                calculation.getErrorCode() == null
                        ? null
                        : new RouteCalculationErrorResponse(
                        calculation.getErrorCode(),
                        calculation.getErrorMessage()
                );

        return new RouteCalculationResponse(
                calculation.getId(),
                calculation.getStatus(),
                new RoutePointResponse(
                        calculation.getStartLabel(),
                        calculation.getStartLatitude(),
                        calculation.getStartLongitude()
                ),
                new RoutePointResponse(
                        calculation.getDestinationLabel(),
                        calculation.getDestinationLatitude(),
                        calculation.getDestinationLongitude()
                ),
                new ActivityProfileResponse(
                        calculation.getActivityProfileCode(),
                        calculation.getRoutingProfile()
                ),
                calculation.getDepartureTime(),
                calculation.getArrivalTime(),
                calculation.getDistanceMeters(),
                calculation.getDurationSeconds(),
                readJson(
                        calculation.getGeometry(),
                        objectMapper
                ),
                readJson(
                        calculation.getSegments(),
                        objectMapper
                ),
                calculation.getCreatedAt(),
                calculation.getUpdatedAt(),
                error
        );
    }

    private static JsonNode readJson(
            String value,
            ObjectMapper objectMapper
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return objectMapper.readTree(value);
    }

    public record RoutePointResponse(
            String label,
            double latitude,
            double longitude
    ) {
    }

    public record ActivityProfileResponse(
            ActivityProfileCode code,
            String routingProfile
    ) {
    }
}