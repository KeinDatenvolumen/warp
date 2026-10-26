package de.warp.bff.infrastructure.client.dto;

import java.time.Instant;

public record InternalSessionResponse(
        String id,
        String userId,
        Instant createdAt,
        Instant expiresAt,
        boolean emailVerified
) {
}