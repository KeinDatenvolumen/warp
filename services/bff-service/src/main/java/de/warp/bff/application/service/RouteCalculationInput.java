package de.warp.bff.application.service;

import java.time.Instant;

public record RouteCalculationInput(
        RoutePoint start,
        RoutePoint destination,
        Instant departureTime,
        String activityProfile
) {

    public record RoutePoint(
            String label,
            double latitude,
            double longitude
    ) {
    }
}