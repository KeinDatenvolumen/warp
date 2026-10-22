package de.warp.bff.infrastructure.routing.dto;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

public record InternalRouteCalculationResponse(
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
        Instant updatedAt,
        RouteCalculationError error
) {

    public record RoutePoint(
            String label,
            double latitude,
            double longitude
    ) {
    }

    public record ActivityProfileSnapshot(
            String code,
            String routingProfile
    ) {
    }

    public record RouteCalculationError(
            String code,
            String message
    ) {
    }
}