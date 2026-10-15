package de.warp.identity.dto;

import java.time.Instant;

public record CreatedSessionResponse(
        String sessionToken,
        String userId,
        Instant createdAt,
        Instant expiresAt
) {
}