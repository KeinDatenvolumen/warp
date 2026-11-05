// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
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