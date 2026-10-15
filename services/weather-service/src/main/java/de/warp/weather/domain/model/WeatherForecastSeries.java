package de.warp.weather.domain.model;

import java.util.List;
import java.util.Objects;

public record WeatherForecastSeries(
        WeatherModel model,
        double resolvedLatitude,
        double resolvedLongitude,
        double elevationMeters,
        List<WeatherForecastTimeStep> hourly
) {

    public WeatherForecastSeries {
        Objects.requireNonNull(model, "model must not be null");
        hourly = List.copyOf(
                Objects.requireNonNull(hourly, "hourly must not be null")
        );
    }
}