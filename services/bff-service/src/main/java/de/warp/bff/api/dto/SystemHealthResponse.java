package de.warp.bff.api.dto;

import java.time.Instant;
import java.util.List;

public record SystemHealthResponse(
        String status,
        Instant checkedAt,
        int healthyServices,
        int totalServices,
        List<ServiceHealthResponse> services
) {
}