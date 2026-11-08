// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-071] (7a94bc0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-005)
// [KI-Log Nr. KI-088] (58996f4) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add BFF identity session client
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.client.dto;

import java.time.Instant;

public record InternalRegistrationResponse(
        String id,
        String name,
        String email,
        boolean emailVerified,
        Instant createdAt
) {
}