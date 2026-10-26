package de.warp.weather.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "warp.weather.cache")
public record WeatherCacheProperties(
        Duration ttl
) {

    public WeatherCacheProperties {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(60);
        }
    }

    public static WeatherCacheProperties defaults() {
        return new WeatherCacheProperties(Duration.ofMinutes(60));
    }
}
