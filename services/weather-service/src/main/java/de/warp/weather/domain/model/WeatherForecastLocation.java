// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.domain.model;

import java.util.Map;
import java.util.Objects;

public record WeatherForecastLocation(
        ForecastLocation requestedLocation,
        WeatherForecastSeries bestMatch,
        Map<WeatherModel, WeatherForecastSeries> modelForecasts
) {

    public WeatherForecastLocation {
        Objects.requireNonNull(
                requestedLocation,
                "requestedLocation must not be null"
        );

        Objects.requireNonNull(
                bestMatch,
                "bestMatch must not be null"
        );

        modelForecasts = Map.copyOf(
                Objects.requireNonNull(
                        modelForecasts,
                        "modelForecasts must not be null"
                )
        );
    }
}