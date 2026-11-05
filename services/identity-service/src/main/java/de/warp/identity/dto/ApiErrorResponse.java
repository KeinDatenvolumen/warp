// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
package de.warp.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.util.List;

public record ApiErrorResponse(
        URI type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String correlationId,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorResponse> fieldErrors
) {
        public ApiErrorResponse {
                fieldErrors = fieldErrors == null
                        ? List.of()
                        : List.copyOf(fieldErrors);
        }
}