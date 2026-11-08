// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.exception;

import de.warp.bff.infrastructure.client.dto.StorageApiDtos.StorageFieldError;

import java.util.List;

/**
 * Der Storage-Service hat die Eingabe mit 422 abgelehnt. Die Feldfehler
 * werden laut Vertrag in den GraphQL-Payload übertragen.
 */
public class StorageValidationException extends RuntimeException {

    private final transient List<StorageFieldError> fieldErrors;

    public StorageValidationException(
            List<StorageFieldError> fieldErrors,
            Throwable cause
    ) {
        super("Der Speicherdienst hat die Eingabe abgelehnt.", cause);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<StorageFieldError> getFieldErrors() {
        return fieldErrors;
    }
}