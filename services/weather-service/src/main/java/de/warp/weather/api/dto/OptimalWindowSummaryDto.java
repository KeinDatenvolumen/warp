// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.api.dto;

public record OptimalWindowSummaryDto(
        String bestDeparture,
        String bestDepartureIso,
        int bestScore,
        String stableWindow,
        String advantageVsWorst,
        String worstDeparture,
        int worstScore
) {
}
