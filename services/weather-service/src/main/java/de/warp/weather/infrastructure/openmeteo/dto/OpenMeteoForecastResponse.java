// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
