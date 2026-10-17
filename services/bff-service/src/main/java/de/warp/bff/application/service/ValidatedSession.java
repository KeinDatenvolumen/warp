package de.warp.bff.application.service;

import java.time.Instant;

public record ValidatedSession(
        String userId,
        Instant createdAt,
        Instant expiresAt
) {
}