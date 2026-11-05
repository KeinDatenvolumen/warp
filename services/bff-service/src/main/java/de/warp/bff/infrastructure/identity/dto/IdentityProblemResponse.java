// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
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