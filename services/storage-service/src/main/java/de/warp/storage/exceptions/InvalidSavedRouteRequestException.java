// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage.exceptions;

import de.warp.storage.dto.FieldErrorResponse;

import java.util.List;

public class InvalidSavedRouteRequestException extends RuntimeException {

    private final transient List<FieldErrorResponse> fieldErrors;

    public InvalidSavedRouteRequestException(
            String field,
            String code,
            String message
    ) {
        super(message);
        this.fieldErrors = List.of(
                new FieldErrorResponse(field, code, message)
        );
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }
}