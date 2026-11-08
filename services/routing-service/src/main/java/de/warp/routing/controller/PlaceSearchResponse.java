// [KI-Log Nr. KI-123] (00e2e64) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-137] (15d143f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.controller;

import java.util.List;

import de.warp.routing.domain.model.Place;

public record PlaceSearchResponse(
        List < Place > items
) {
}