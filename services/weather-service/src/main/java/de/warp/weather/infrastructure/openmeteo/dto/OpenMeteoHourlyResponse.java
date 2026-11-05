// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.infrastructure.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoHourlyResponse(
        List<Long> time,

        @JsonProperty("temperature_2m")
        List<Double> temperature2m,

        @JsonProperty("apparent_temperature")
        List<Double> apparentTemperature,

        @JsonProperty("relative_humidity_2m")
        List<Double> relativeHumidity2m,

        @JsonProperty("dew_point_2m")
        List<Double> dewPoint2m,

        @JsonProperty("wet_bulb_temperature_2m")
        List<Double> wetBulbTemperature2m,

        @JsonProperty("precipitation_probability")
        List<Double> precipitationProbability,

        List<Double> precipitation,
        List<Double> rain,
        List<Double> showers,
        List<Double> snowfall,

        @JsonProperty("snow_depth")
        List<Double> snowDepth,

        @JsonProperty("wind_speed_10m")
        List<Double> windSpeed10m,

        @JsonProperty("wind_direction_10m")
        List<Double> windDirection10m,

        @JsonProperty("wind_gusts_10m")
        List<Double> windGusts10m,

        @JsonProperty("pressure_msl")
        List<Double> pressureMsl,

        @JsonProperty("surface_pressure")
        List<Double> surfacePressure,

        @JsonProperty("cloud_cover")
        List<Double> cloudCover,

        @JsonProperty("cloud_cover_low")
        List<Double> cloudCoverLow,

        @JsonProperty("cloud_cover_mid")
        List<Double> cloudCoverMid,

        @JsonProperty("cloud_cover_high")
        List<Double> cloudCoverHigh,

        List<Double> visibility,

        @JsonProperty("uv_index")
        List<Double> uvIndex,

        @JsonProperty("uv_index_clear_sky")
        List<Double> uvIndexClearSky,

        @JsonProperty("shortwave_radiation")
        List<Double> shortwaveRadiation,

        @JsonProperty("shortwave_radiation_instant")
        List<Double> shortwaveRadiationInstant,

        @JsonProperty("direct_radiation")
        List<Double> directRadiation,

        @JsonProperty("diffuse_radiation")
        List<Double> diffuseRadiation,

        @JsonProperty("sunshine_duration")
        List<Double> sunshineDuration,

        @JsonProperty("is_day")
        List<Double> isDay,

        List<Double> cape,

        @JsonProperty("lifted_index")
        List<Double> liftedIndex,

        @JsonProperty("convective_inhibition")
        List<Double> convectiveInhibition,

        @JsonProperty("freezing_level_height")
        List<Double> freezingLevelHeight,

        @JsonProperty("vapour_pressure_deficit")
        List<Double> vapourPressureDeficit,

        List<Double> evapotranspiration,

        @JsonProperty("soil_temperature_0cm")
        List<Double> soilTemperature0cm,

        @JsonProperty("soil_temperature_0_to_7cm")
        List<Double> soilTemperature0To7cm,

        @JsonProperty("weather_code")
        List<Double> weatherCode
) {
}
