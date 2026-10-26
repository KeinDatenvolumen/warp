package de.warp.storage.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GeoJsonLineString(
        @NotNull(message = "Der Geometrietyp muss angegeben werden.")
        @Pattern(
                regexp = "LineString",
                message = "Es wird nur der Geometrietyp LineString unterstützt."
        )
        String type,

        @NotNull(message = "Die Koordinaten müssen angegeben werden.")
        @Size(
                max = 100000,
                message = "Eine Linie darf maximal 100000 Positionen enthalten."
        )
        List<@NotNull @Size(min = 2, max = 3) List<@NotNull Double>> coordinates
) {
}