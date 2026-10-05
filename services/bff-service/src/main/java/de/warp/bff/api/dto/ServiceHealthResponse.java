package de.warp.bff.api.dto;

public record ServiceHealthResponse(
        String service,
        String status,
        boolean reachable,
        long responseTimeMs,
        String checkedUrl
) {
}