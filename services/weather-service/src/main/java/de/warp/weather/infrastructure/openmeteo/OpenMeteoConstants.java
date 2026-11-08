// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.infrastructure.openmeteo;

import java.util.List;

final class OpenMeteoConstants {

    static final List<String> HOURLY_VARIABLES = List.of(
            "temperature_2m",
            "apparent_temperature",
            "relative_humidity_2m",
            "dew_point_2m",
            "wet_bulb_temperature_2m",

            "precipitation_probability",
            "precipitation",
            "rain",
            "showers",
            "snowfall",
            "snow_depth",

            "wind_speed_10m",
            "wind_direction_10m",
            "wind_gusts_10m",

            "pressure_msl",
            "surface_pressure",

            "cloud_cover",
            "cloud_cover_low",
            "cloud_cover_mid",
            "cloud_cover_high",
            "visibility",

            "uv_index",
            "uv_index_clear_sky",
            "shortwave_radiation",
            "shortwave_radiation_instant",
            "direct_radiation",
            "diffuse_radiation",
            "sunshine_duration",
            "is_day",

            "cape",
            "lifted_index",
            "convective_inhibition",
            "freezing_level_height",

            "vapour_pressure_deficit",
            "evapotranspiration",
            "soil_temperature_0cm",
            "soil_temperature_0_to_7cm",

            "weather_code"
    );

    static final List<String> MINUTELY_15_VARIABLES = List.of(
            "temperature_2m",
            "apparent_temperature",
            "relative_humidity_2m",
            "dew_point_2m",
            "shortwave_radiation_instant",
            "precipitation",
            "snowfall",
            "wind_speed_10m",
            "wind_direction_10m",
            "wind_gusts_10m",
            "weather_code",
            "visibility",
            "cape",
            "is_day"
    );

    static final String HOURLY_PARAMETER =
            String.join(",", HOURLY_VARIABLES);

    static final String MINUTELY_15_PARAMETER =
            String.join(",", MINUTELY_15_VARIABLES);

    private OpenMeteoConstants() {
    }
}
