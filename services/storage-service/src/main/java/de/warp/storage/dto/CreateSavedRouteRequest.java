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