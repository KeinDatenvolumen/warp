// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.Objects;

public record WeatherForecastTimeStep(
        Instant time,
        WeatherValues values
) {

    public WeatherForecastTimeStep {
        Objects.requireNonNull(time, "time must not be null");
        Objects.requireNonNull(values, "values must not be null");
    }
}