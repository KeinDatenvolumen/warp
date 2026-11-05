// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
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
