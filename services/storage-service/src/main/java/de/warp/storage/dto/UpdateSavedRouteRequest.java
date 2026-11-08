// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSavedRouteRequest(
        @Size(max = 100, message = "Der Routenname darf maximal 100 Zeichen enthalten.")
        @Pattern(
                regexp = ".*\\S.*",
                message = "Der Routenname darf nicht leer sein."
        )
        String name,

        Boolean favorite
) {
    public UpdateSavedRouteRequest {
        if (name != null) {
            name = name.trim().isEmpty() ? " " : name.trim();
        }
    }
}