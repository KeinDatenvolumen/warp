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

            "weather_code"
    );

    static final String HOURLY_PARAMETER =
            String.join(",", HOURLY_VARIABLES);

    private OpenMeteoConstants() {
    }
}