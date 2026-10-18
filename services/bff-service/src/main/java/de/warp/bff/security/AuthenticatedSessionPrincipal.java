package de.warp.bff.security;

import java.time.Instant;

public record AuthenticatedSessionPrincipal(
        String userId,
        Instant createdAt,
        Instant expiresAt
) {
}