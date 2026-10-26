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

    @Column(name = "wind_speed_kmh")
    private Double windSpeedKmh;

    @Column(name = "wind_direction_degrees")
    private Double windDirectionDegrees;

    @Column(name = "precipitation_probability")
    private Double precipitationProbabilityPercent;

    @Column(name = "precipitation_mm")
    private Double precipitationMm;

    @Column(name = "thunderstorm_probability")
    private Double thunderstormProbabilityPercent;

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
        this.windSpeedKmh = condition.windSpeedKmh();
        this.windDirectionDegrees = condition.windDirectionDegrees();
        this.precipitationProbabilityPercent = condition.precipitationProbabilityPercent();
        this.precipitationMm = condition.precipitationMm();
        this.thunderstormProbabilityPercent = condition.thunderstormProbabilityPercent();
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
                        precipitationProbabilityPercent,
                        precipitationMm,
                        thunderstormProbabilityPercent,
                        temperatureCelsius,
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
