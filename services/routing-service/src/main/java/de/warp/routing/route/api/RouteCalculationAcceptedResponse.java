package de.warp.routing.route.api;

import java.time.Instant;

import de.warp.routing.route.domain.RouteCalculationStatus;

public record RouteCalculationAcceptedResponse(
        String id,
        RouteCalculationStatus status,
        Instant createdAt
) {
}