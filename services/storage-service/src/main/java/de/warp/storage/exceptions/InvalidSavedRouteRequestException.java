// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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