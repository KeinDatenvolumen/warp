// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
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

        OpenMeteoHourlyResponse hourly,

        @JsonProperty("minutely_15")
        OpenMeteoHourlyResponse minutely15
) {
    public OpenMeteoForecastResponse(
            Double latitude,
            Double longitude,
            Double elevation,
            Double generationTimeMilliseconds,
            Integer utcOffsetSeconds,
            String timezone,
            String timezoneAbbreviation,
            OpenMeteoHourlyResponse hourly
    ) {
        this(
                latitude,
                longitude,
                elevation,
                generationTimeMilliseconds,
                utcOffsetSeconds,
                timezone,
                timezoneAbbreviation,
                hourly,
                null
        );
    }
}
