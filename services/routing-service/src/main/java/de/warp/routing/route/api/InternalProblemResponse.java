// [KI-Log Nr. KI-160] (98b4bc2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 handle internal route calculation errors
// [KI-Log Nr. KI-176] (622e888) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 handle internal route calculation errors
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.route.api;

import java.util.List;

public record InternalProblemResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String correlationId,
        List<FieldErrorResponse> fieldErrors
) {

    public record FieldErrorResponse(
            String field,
            String code,
            String message
    ) {
    }
}