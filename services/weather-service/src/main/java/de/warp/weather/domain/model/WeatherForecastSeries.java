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
