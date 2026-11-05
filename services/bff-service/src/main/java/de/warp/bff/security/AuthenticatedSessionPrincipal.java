// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.security;

import java.time.Instant;

public record AuthenticatedSessionPrincipal(
        String userId,
        Instant createdAt,
        Instant expiresAt,
        boolean emailVerified
) {
}