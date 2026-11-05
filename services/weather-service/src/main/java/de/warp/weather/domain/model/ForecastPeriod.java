// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record ForecastPeriod(
        Instant start,
        Instant end
) {

    public ForecastPeriod {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "end must be after start"
            );
        }

        if (start.truncatedTo(ChronoUnit.HOURS)
                .plus(16, ChronoUnit.DAYS)
                .isBefore(end)) {
            throw new IllegalArgumentException(
                    "forecast period must not exceed 16 days"
            );
        }
    }
}