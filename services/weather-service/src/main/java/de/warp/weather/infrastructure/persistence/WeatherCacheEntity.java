package de.warp.weather.infrastructure.persistence;

import de.warp.weather.domain.model.CachedForecast;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "weather_cache", schema = "warp_weather")
public class WeatherCacheEntity {

    @Id
    @Column(name = "cache_key", nullable = false, length = 120)
    private String cacheKey;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "forecast_time", nullable = false)
    private Instant forecastTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_model", nullable = false, length = 32)
    private WeatherModel weatherModel;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "apparent_temperature_celsius")
    private Double apparentTemperatureCelsius;

    @Column(name = "relative_humidity_percent")
    private Double relativeHumidityPercent;

    @Column(name = "dew_point_celsius")
    private Double dewPointCelsius;

    @Column(name = "shortwave_radiation_wm2")
    private Double shortwaveRadiationWm2;

    @Column(name = "soil_temperature_celsius")
    private Double soilTemperatureCelsius;

    @Column(name = "wind_speed_kmh")
    private Double windSpeedKmh;

    @Column(name = "wind_direction_degrees")
    private Double windDirectionDegrees;

    @Column(name = "wind_gusts_kmh")
    private Double windGustsKmh;

    @Column(name = "precipitation_probability")
    private Double precipitationProbabilityPercent;

    @Column(name = "precipitation_mm")
    private Double precipitationMm;

    @Column(name = "wet_road_index")
    private Double wetRoadIndex;

    @Column(name = "snowfall_cm")
    private Double snowfallCm;

    @Column(name = "thunderstorm_probability")
    private Double thunderstormProbabilityPercent;

    @Column(name = "visibility_meters")
    private Double visibilityMeters;

    @Column(name = "cape_jkg")
    private Double capeJkg;

    @Column(name = "uv_index")
    private Double uvIndex;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "cached_at", nullable = false)
    private Instant cachedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected WeatherCacheEntity() {
    }

    public WeatherCacheEntity(
            String cacheKey,
            double latitude,
            double longitude,
            Instant forecastTime,
            WeatherModel weatherModel,
            WeatherCondition condition,
            Instant cachedAt,
            Instant expiresAt
    ) {
        this.cacheKey = cacheKey;
        this.latitude = latitude;
        this.longitude = longitude;
        this.forecastTime = forecastTime;
        this.weatherModel = weatherModel;
        this.temperatureCelsius = condition.temperatureCelsius();
        this.apparentTemperatureCelsius = condition.apparentTemperatureCelsius();
        this.relativeHumidityPercent = condition.relativeHumidityPercent();
        this.dewPointCelsius = condition.dewPointCelsius();
        this.shortwaveRadiationWm2 = condition.shortwaveRadiationWm2();
        this.soilTemperatureCelsius = condition.soilTemperatureCelsius();
        this.windSpeedKmh = condition.windSpeedKmh();
        this.windDirectionDegrees = condition.windDirectionDegrees();
        this.windGustsKmh = condition.windGustsKmh();
        this.precipitationProbabilityPercent = condition.precipitationProbabilityPercent();
        this.precipitationMm = condition.precipitationMm();
        this.wetRoadIndex = condition.wetRoadIndex();
        this.snowfallCm = condition.snowfallCm();
        this.thunderstormProbabilityPercent = condition.thunderstormProbabilityPercent();
        this.visibilityMeters = condition.visibilityMeters();
        this.capeJkg = condition.capeJkg();
        this.uvIndex = condition.uvIndex();
        this.weatherCode = condition.weatherCode();
        this.cachedAt = cachedAt;
        this.expiresAt = expiresAt;
    }

    public CachedForecast toDomain() {
        return new CachedForecast(
                cacheKey,
                latitude,
                longitude,
                forecastTime,
                cachedAt,
                expiresAt,
                weatherModel,
                new WeatherCondition(
                        windSpeedKmh,
                        windDirectionDegrees,
                        windGustsKmh,
                        precipitationProbabilityPercent,
                        precipitationMm,
                        wetRoadIndex,
                        snowfallCm,
                        thunderstormProbabilityPercent,
                        temperatureCelsius,
                        apparentTemperatureCelsius,
                        relativeHumidityPercent,
                        dewPointCelsius,
                        shortwaveRadiationWm2,
                        soilTemperatureCelsius,
                        visibilityMeters,
                        capeJkg,
                        uvIndex,
                        weatherCode
                )
        );
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getForecastTime() {
        return forecastTime;
    }

    public WeatherModel getWeatherModel() {
        return weatherModel;
    }

    public Instant getCachedAt() {
        return cachedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
