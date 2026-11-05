// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.api.dto;

import java.time.Instant;

public record CreateSessionResponse(
        String userId,
        Instant expiresAt
) {
}