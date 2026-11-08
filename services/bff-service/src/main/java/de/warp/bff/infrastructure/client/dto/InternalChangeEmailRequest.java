// [KI-Log Nr. KI-088] (58996f4) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add BFF identity session client
// [KI-Log Nr. KI-203] (e8a363f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add user lookup and account change endpoints to identity service (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.client.dto;

public record InternalChangeEmailRequest(
        String newEmail,
        String password
) {
}