package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CachedForecast(
        String cacheKey,
        double latitude,
        double longitude,
        Instant forecastTime,
        Instant cachedAt,
        Instant expiresAt,
        WeatherModel weatherModel,
        WeatherCondition condition
) {

    public CachedForecast {
        Objects.requireNonNull(cacheKey, "cacheKey must not be null");
        if (cacheKey.isBlank()) {
            throw new IllegalArgumentException("cacheKey must not be blank");
        }
        Objects.requireNonNull(forecastTime, "forecastTime must not be null");
        Objects.requireNonNull(cachedAt, "cachedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(cachedAt)) {
            throw new IllegalArgumentException("expiresAt must be after cachedAt");
        }
        Objects.requireNonNull(weatherModel, "weatherModel must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return !expiresAt.isAfter(now);
    }
}
