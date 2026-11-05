// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage.dto;

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
        List<FieldErrorResponse> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }
}