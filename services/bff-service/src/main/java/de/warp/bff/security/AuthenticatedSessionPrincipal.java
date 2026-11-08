// [KI-Log Nr. KI-082] (d939743) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: implement session creation service
// [KI-Log Nr. KI-086] (f458eed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add session validation and invalidation
// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-200] (c059ad7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: require verified email for route calculation (WARP-030)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.security;

import java.time.Instant;

public record AuthenticatedSessionPrincipal(
        String userId,
        Instant createdAt,
        Instant expiresAt,
        boolean emailVerified
) {
}