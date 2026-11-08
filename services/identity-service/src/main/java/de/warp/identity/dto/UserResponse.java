// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-071] (7a94bc0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-005)
// [KI-Log Nr. KI-091] (fc545e3) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: complete success response and automated coverage (WARP-005)
// [KI-Log Nr. KI-094] (a3d7118) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: remove message field from registration response (WARP-005)
// [KI-Log Nr. KI-112] (88962a1) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: complete success response and automated coverage (WARP-005)
// [KI-Log Nr. KI-115] (434109e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: remove message field from registration response (WARP-005)
// [KI-Log Nr. KI-202] (d555a4e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add user lookup and account change endpoints to identity service (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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