package de.warp.bff.infrastructure.routing.dto;

import java.time.Instant;

public record InternalRouteCalculationAcceptedResponse(
        String id,
        String status,
        Instant createdAt
) {
}