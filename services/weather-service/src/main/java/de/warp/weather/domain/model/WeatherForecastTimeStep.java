// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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