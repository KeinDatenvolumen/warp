package de.warp.bff.infrastructure.client.dto;

import java.time.Instant;

public record InternalCreatedSessionResponse(
        String sessionToken,
        String userId,
        Instant createdAt,
        Instant expiresAt
) {
}