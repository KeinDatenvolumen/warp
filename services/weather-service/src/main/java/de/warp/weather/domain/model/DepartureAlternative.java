package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.Objects;

public record DepartureAlternative(
        Instant departureTime,
        Instant expectedArrivalTime,
        int weatherScore,
        boolean bestDeparture
) {

    public DepartureAlternative {
        Objects.requireNonNull(departureTime, "departureTime must not be null");
        Objects.requireNonNull(expectedArrivalTime, "expectedArrivalTime must not be null");
        if (weatherScore < 0 || weatherScore > 100) {
            throw new IllegalArgumentException("weatherScore must be between 0 and 100");
        }
    }
}
