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