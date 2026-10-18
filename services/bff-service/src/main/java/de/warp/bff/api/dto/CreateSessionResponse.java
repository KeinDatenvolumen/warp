package de.warp.bff.api.dto;

import java.time.Instant;

public record CreateSessionResponse(
        String userId,
        Instant expiresAt
) {
}