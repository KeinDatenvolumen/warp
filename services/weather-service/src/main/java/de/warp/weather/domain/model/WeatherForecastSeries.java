// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.domain.model;

import java.util.List;
import java.util.Objects;

public record WeatherForecastSeries(
        WeatherModel model,
        double resolvedLatitude,
        double resolvedLongitude,
        double elevationMeters,
        List<WeatherForecastTimeStep> hourly,
        List<WeatherForecastTimeStep> minutely15
) {

    public WeatherForecastSeries {
        Objects.requireNonNull(model, "model must not be null");
        hourly = List.copyOf(
                Objects.requireNonNull(hourly, "hourly must not be null")
        );
        minutely15 = minutely15 == null ? List.of() : List.copyOf(minutely15);
    }

    public WeatherForecastSeries(
            WeatherModel model,
            double resolvedLatitude,
            double resolvedLongitude,
            double elevationMeters,
            List<WeatherForecastTimeStep> hourly
    ) {
        this(model, resolvedLatitude, resolvedLongitude, elevationMeters, hourly, List.of());
    }
}
