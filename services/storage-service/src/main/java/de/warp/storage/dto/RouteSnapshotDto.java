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