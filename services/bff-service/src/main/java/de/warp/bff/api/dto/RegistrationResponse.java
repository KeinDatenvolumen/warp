package de.warp.bff.api.dto;

import java.time.Instant;

public record RegistrationResponse(
        String id,
        String name,
        String email,
        boolean emailVerified,
        Instant createdAt,
        String message
) {
}