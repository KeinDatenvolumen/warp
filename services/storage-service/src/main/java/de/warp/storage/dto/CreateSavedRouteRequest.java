// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSavedRouteRequest(
        @NotBlank(message = "Der Routenname darf nicht leer sein.")
        @Size(max = 100, message = "Der Routenname darf maximal 100 Zeichen enthalten.")
        String name,

        boolean favorite,

        @NotNull @Valid RouteSnapshotDto routeSnapshot,

        @NotNull @Valid WeatherSnapshotDto weatherSnapshot
) {
    public CreateSavedRouteRequest {
        if (name != null) {
            name = name.trim();
        }
    }
}