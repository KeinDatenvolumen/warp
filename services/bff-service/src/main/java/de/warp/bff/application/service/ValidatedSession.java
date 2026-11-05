// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.application.service;

import java.time.Instant;

public record ValidatedSession(
        String userId,
        Instant createdAt,
        Instant expiresAt,
        boolean emailVerified
) {
}