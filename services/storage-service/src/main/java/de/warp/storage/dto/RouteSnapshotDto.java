// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
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