// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.infrastructure.client.dto;

import java.time.Instant;

public record InternalCreatedSessionResponse(
        String sessionToken,
        String userId,
        Instant createdAt,
        Instant expiresAt
) {
}