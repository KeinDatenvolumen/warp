// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
package de.warp.identity.dto;

import java.time.Instant;

public record UserResponse(
        String id,
        String name,
        String email,
        boolean emailVerified,
        Instant createdAt
) {
}