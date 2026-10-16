package de.warp.identity.dto;

import java.time.Instant;

public record RegistrationResponse(
        String id,
        String name,
        String email,
        boolean emailVerified,
        Instant createdAt
) {
}