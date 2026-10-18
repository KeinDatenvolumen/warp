package de.warp.bff.infrastructure.client.dto;

import java.time.Instant;

public record InternalRegistrationResponse(
        String id,
        String name,
        String email,
        boolean emailVerified,
        Instant createdAt
) {
}