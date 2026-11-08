// [KI-Log Nr. KI-156] (7fb0636) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 persist route calculations
// [KI-Log Nr. KI-172] (45536d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 persist route calculations
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.route.domain;

public enum RouteCalculationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}