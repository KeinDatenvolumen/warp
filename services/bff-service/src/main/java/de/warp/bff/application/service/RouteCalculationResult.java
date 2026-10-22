package de.warp.bff.application.service;

import java.time.Instant;

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
}