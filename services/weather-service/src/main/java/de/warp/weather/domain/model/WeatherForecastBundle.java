package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WeatherForecastBundle(
        Instant retrievedAt,
        List<WeatherForecastLocation> locations
) {

    public WeatherForecastBundle {
        Objects.requireNonNull(
                retrievedAt,
                "retrievedAt must not be null"
        );

        locations = List.copyOf(
                Objects.requireNonNull(
                        locations,
                        "locations must not be null"
                )
        );
    }
}