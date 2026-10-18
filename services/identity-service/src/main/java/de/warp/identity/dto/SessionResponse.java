package de.warp.identity.dto;

import java.time.Instant;

public record SessionResponse(
        String id,
        String userId,
        Instant createdAt,
        Instant expiresAt
) {
}