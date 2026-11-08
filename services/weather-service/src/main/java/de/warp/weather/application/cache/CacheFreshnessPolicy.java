// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.cache;

import de.warp.weather.domain.model.WeatherModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

@Component
public class CacheFreshnessPolicy {

    public Instant normalizeToForecastHour(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Instant truncated = timestamp.truncatedTo(ChronoUnit.HOURS);
        long minutesIntoHour = Duration.between(truncated, timestamp).toMinutes();
        return minutesIntoHour >= 30
                ? truncated.plus(1, ChronoUnit.HOURS)
                : truncated;
    }

    public Instant normalizeToQuarterHour(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        long epochSeconds = timestamp.getEpochSecond();
        long roundedSeconds = Math.round((double) epochSeconds / 900.0) * 900L;
        return Instant.ofEpochSecond(roundedSeconds);
    }

    public Instant ceilToQuarterHour(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        long epochSeconds = timestamp.getEpochSecond();
        long ceiledSeconds = ((epochSeconds + 899L) / 900L) * 900L;
        return Instant.ofEpochSecond(ceiledSeconds);
    }

    public double roundCoordinate(double coordinate) {
        return Math.round(coordinate * 10_000.0) / 10_000.0;
    }

    public String buildCacheKey(
            double latitude,
            double longitude,
            Instant forecastTime,
            WeatherModel model
    ) {
        Objects.requireNonNull(forecastTime, "forecastTime must not be null");
        WeatherModel effectiveModel = model == null ? WeatherModel.BEST_MATCH : model;
        Instant normalizedHour = normalizeToForecastHour(forecastTime);

        return String.format(
                Locale.ROOT,
                "%s:%.4f:%.4f:%s",
                effectiveModel.name(),
                roundCoordinate(latitude),
                roundCoordinate(longitude),
                normalizedHour.toString()
        );
    }

    public boolean isExpired(Instant cachedAt, Duration ttl, Instant now) {
        Objects.requireNonNull(cachedAt, "cachedAt must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return now.isAfter(cachedAt.plus(ttl));
    }
}
