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
