// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RouteSnapshotDto(
        @Size(max = 40) String routeCalculationId,

        @NotNull @Valid PlaceDto start,

        @NotNull @Valid PlaceDto destination,

        @NotNull
        @Pattern(
                regexp = "BICYCLE|MOTORCYCLE|HIKING",
                message = "Das Aktivitätsprofil ist ungültig."
        )
        String activityProfileCode,

        @NotNull OffsetDateTime departureTime,

        @NotNull OffsetDateTime arrivalTime,

        @NotNull @PositiveOrZero Integer distanceMeters,

        @NotNull @PositiveOrZero Integer durationSeconds,

        @NotNull @Valid GeoJsonLineString geometry
) {
}