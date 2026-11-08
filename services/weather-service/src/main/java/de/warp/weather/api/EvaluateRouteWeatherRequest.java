// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.api;

import de.warp.weather.domain.model.RouteSnapshot;

import java.time.Instant;
import java.util.List;

public record EvaluateRouteWeatherRequest(
        String requestedByUserId,
        RouteSnapshotRequest routeSnapshot
) {

    public RouteSnapshot toDomain() {
        if (routeSnapshot == null) {
            throw new IllegalArgumentException("routeSnapshot must not be null");
        }
        return routeSnapshot.toDomain();
    }

    public record RouteSnapshotRequest(
            String routeCalculationId,
            Instant departureTime,
            int durationSeconds,
            int distanceMeters,
            List<List<Double>> coordinates,
            ActivityProfileSnapshotRequest activityProfile
    ) {

        public RouteSnapshot toDomain() {
            if (activityProfile == null) {
                throw new IllegalArgumentException("activityProfile must not be null");
            }
            return new RouteSnapshot(
                    routeCalculationId,
                    departureTime,
                    durationSeconds,
                    distanceMeters,
                    coordinates,
                    new RouteSnapshot.ActivityProfileSnapshot(
                            activityProfile.code(),
                            activityProfile.averageSpeedKmh() != null
                                    ? activityProfile.averageSpeedKmh()
                                    : 0.0,
                            activityProfile.weatherQueryIntervalKm() != null
                                    ? activityProfile.weatherQueryIntervalKm()
                                    : 0
                    )
            );
        }
    }

    public record ActivityProfileSnapshotRequest(
            String code,
            Double averageSpeedKmh,
            Integer weatherQueryIntervalKm
    ) {
    }
}
