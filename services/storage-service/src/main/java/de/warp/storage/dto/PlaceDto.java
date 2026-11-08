// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceDto(
        @NotBlank(message = "Die Ortsbezeichnung darf nicht leer sein.")
        @Size(max = 255, message = "Die Ortsbezeichnung ist zu lang.")
        String label,

        @NotNull(message = "Der Breitengrad muss angegeben werden.")
        @DecimalMin(value = "-90.0", message = "Ungültiger Breitengrad.")
        @DecimalMax(value = "90.0", message = "Ungültiger Breitengrad.")
        Double latitude,

        @NotNull(message = "Der Längengrad muss angegeben werden.")
        @DecimalMin(value = "-180.0", message = "Ungültiger Längengrad.")
        @DecimalMax(value = "180.0", message = "Ungültiger Längengrad.")
        Double longitude
) {
}