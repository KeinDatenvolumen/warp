// [KI-Log Nr. KI-237] (d313c4e) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-035 improve code, implement best practises
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import de.warp.identity.entity.SessionEntity;

import java.time.Instant;

public record CachedSession(
        String id,
        String userId,
        Instant createdAt,
        Instant expiresAt,
        Instant invalidatedAt,
        boolean emailVerified
) {
    public boolean isValidAt(Instant instant) {
        return invalidatedAt == null && expiresAt.isAfter(instant);
    }

    public static CachedSession from(SessionEntity session) {
        return new CachedSession(
                session.getId(),
                session.getUser().getId(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                session.getInvalidatedAt(),
                session.getUser().isEmailVerified()
        );
    }
}
