package de.warp.weather.infrastructure.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoForecastResponse(
        Double latitude,
        Double longitude,
        Double elevation,

        @JsonProperty("generationtime_ms")
        Double generationTimeMilliseconds,

        @JsonProperty("utc_offset_seconds")
        Integer utcOffsetSeconds,

        String timezone,

        @JsonProperty("timezone_abbreviation")
        String timezoneAbbreviation,

        OpenMeteoHourlyResponse hourly
) {
}