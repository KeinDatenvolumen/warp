// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;

public record WeatherSegmentDto(
        @NotNull @PositiveOrZero Integer segmentIndex,
        @NotNull OffsetDateTime expectedArrivalTime,
        @NotNull @Min(0) @Max(100) Integer score,
        @NotNull
        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Die Farbe muss das Format #RRGGBB haben."
        )
        String color,
        @Valid SegmentWeatherDto weather,
        @Valid GeoJsonLineString geometry
) {
}