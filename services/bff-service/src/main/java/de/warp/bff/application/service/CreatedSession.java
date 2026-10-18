package de.warp.bff.application.service;

import java.time.Instant;

public record CreatedSession(
        String sessionToken,
        String userId,
        Instant expiresAt
) {
}