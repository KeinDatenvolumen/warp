// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.identity.dto;

import de.warp.bff.api.dto.FieldErrorResponse;
import java.util.List;

public record IdentityProblemResponse(
        String type,
        String title,
        Integer status,
        String code,
        String detail,
        String instance,
        String correlationId,
        List<FieldErrorResponse> fieldErrors
) {
    public List<FieldErrorResponse> safeFieldErrors() {
        return fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }
}