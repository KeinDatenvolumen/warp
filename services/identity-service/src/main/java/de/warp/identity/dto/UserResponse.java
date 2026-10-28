package de.warp.identity.dto;

import java.time.Instant;

public record UserResponse(
        String id,
        String name,
        String email,
        boolean emailVerified,
        Instant createdAt
) {
}