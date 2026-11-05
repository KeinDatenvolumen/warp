// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RouteSnapshot(
        String routeCalculationId,
        Instant departureTime,
        int durationSeconds,
        int distanceMeters,
        List<List<Double>> coordinates,
        ActivityProfileSnapshot activityProfile
) {

    public RouteSnapshot {
        Objects.requireNonNull(routeCalculationId, "routeCalculationId must not be null");
        if (routeCalculationId.isBlank()) {
            throw new IllegalArgumentException("routeCalculationId must not be blank");
        }
        Objects.requireNonNull(departureTime, "departureTime must not be null");
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds must not be negative");
        }
        if (distanceMeters < 0) {
            throw new IllegalArgumentException("distanceMeters must not be negative");
        }
        Objects.requireNonNull(coordinates, "coordinates must not be null");
        if (coordinates.size() < 2) {
            throw new IllegalArgumentException("coordinates must contain at least 2 points");
        }
        coordinates = coordinates.stream()
                .map(point -> {
                    Objects.requireNonNull(point, "coordinate point must not be null");
                    if (point.size() < 2) {
                        throw new IllegalArgumentException("coordinate point must have [longitude, latitude]");
                    }
                    return List.copyOf(point);
                })
                .toList();
        Objects.requireNonNull(activityProfile, "activityProfile must not be null");
    }

    public record ActivityProfileSnapshot(
            String code,
            double averageSpeedKmh,
            int weatherQueryIntervalKm
    ) {
        public ActivityProfileSnapshot {
            Objects.requireNonNull(code, "code must not be null");
            if (code.isBlank()) {
                throw new IllegalArgumentException("code must not be blank");
            }
            if (averageSpeedKmh <= 0.0) {
                averageSpeedKmh = defaultSpeedForCode(code);
            }
            if (weatherQueryIntervalKm <= 0) {
                weatherQueryIntervalKm = defaultIntervalForCode(code);
            }
        }

        private static double defaultSpeedForCode(String code) {
            return switch (code.toUpperCase()) {
                case "HIKING" -> 5.0;
                case "MOTORCYCLE" -> 60.0;
                default -> 18.0;
            };
        }

        private static int defaultIntervalForCode(String code) {
            return switch (code.toUpperCase()) {
                case "HIKING" -> 2;
                case "MOTORCYCLE" -> 15;
                default -> 5;
            };
        }
    }
}
