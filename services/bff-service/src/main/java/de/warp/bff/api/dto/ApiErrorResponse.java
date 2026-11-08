// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-068] (a7d6e5a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/WARP-005: implement user registration
// [KI-Log Nr. KI-071] (7a94bc0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-005)
// [KI-Log Nr. KI-096] (77d0318) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add public BFF login endpoint
// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        URI type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorResponse> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }
}