package de.warp.bff.application.service;

import java.time.Instant;

public record StartedRouteCalculation(
        String id,
        String status,
        Instant createdAt
) {
}