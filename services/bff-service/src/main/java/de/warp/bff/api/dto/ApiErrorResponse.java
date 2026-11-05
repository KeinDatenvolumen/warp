// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
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