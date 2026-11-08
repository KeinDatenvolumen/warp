// [KI-Log Nr. KI-082] (d939743) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: implement session creation service
// [KI-Log Nr. KI-096] (77d0318) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add public BFF login endpoint
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.service;

import java.time.Instant;

public record CreatedSession(
        String sessionToken,
        String userId,
        Instant expiresAt
) {
}