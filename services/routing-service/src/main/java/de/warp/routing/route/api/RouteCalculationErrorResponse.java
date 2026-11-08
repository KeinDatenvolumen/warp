// [KI-Log Nr. KI-159] (906871f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-175] (b18893e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.route.api;

public record RouteCalculationErrorResponse(
        String code,
        String message
) {
}