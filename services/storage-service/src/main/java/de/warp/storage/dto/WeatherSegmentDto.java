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