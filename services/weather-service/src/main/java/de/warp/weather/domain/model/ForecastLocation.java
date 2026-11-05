// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.domain.model;

import java.util.Objects;

public record ForecastLocation(
        String id,
        double latitude,
        double longitude,
        Double elevationMeters
) {

    public ForecastLocation {
        Objects.requireNonNull(id, "id must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    "latitude must be between -90 and 90"
            );
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    "longitude must be between -180 and 180"
            );
        }

        if (elevationMeters != null
                && (elevationMeters < -500.0
                || elevationMeters > 9000.0)) {
            throw new IllegalArgumentException(
                    "elevationMeters is outside the supported range"
            );
        }
    }
}