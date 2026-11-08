// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
