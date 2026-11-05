// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.application;

import de.warp.weather.domain.model.RouteSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class RouteSegmenter {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final int MAX_SEGMENTS = 50;

    public List<PlannedRouteSegment> segmentRoute(RouteSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        List<List<Double>> coords = snapshot.coordinates();
        int pointCount = coords.size();

        double[] cumulativeDistances = new double[pointCount];
        cumulativeDistances[0] = 0.0;

        for (int i = 1; i < pointCount; i++) {
            List<Double> prev = coords.get(i - 1);
            List<Double> curr = coords.get(i);
            double segmentDist = haversineMeters(
                    prev.get(1), prev.get(0),
                    curr.get(1), curr.get(0)
            );
            cumulativeDistances[i] = cumulativeDistances[i - 1] + segmentDist;
        }

        double polylineDistanceMeters = cumulativeDistances[pointCount - 1];
        double effectiveTotalDistanceMeters = snapshot.distanceMeters() > 0
                ? snapshot.distanceMeters()
                : polylineDistanceMeters;

        double intervalMeters = Math.max(
                500.0,
                snapshot.activityProfile().weatherQueryIntervalKm() * 1000.0
        );

        int segmentCount;
        if (effectiveTotalDistanceMeters <= 0.0) {
            segmentCount = 1;
        } else {
            segmentCount = Math.max(
                    1,
                    (int) Math.ceil(effectiveTotalDistanceMeters / intervalMeters)
            );
            if (segmentCount > MAX_SEGMENTS) {
                segmentCount = MAX_SEGMENTS;
                intervalMeters = effectiveTotalDistanceMeters / segmentCount;
            }
        }

        double scaleToPolyline = effectiveTotalDistanceMeters > 0.0
                ? (polylineDistanceMeters / effectiveTotalDistanceMeters)
                : 1.0;

        double totalDurationSeconds = resolveTotalDurationSeconds(
                snapshot,
                effectiveTotalDistanceMeters
        );

        List<PlannedRouteSegment> segments = new ArrayList<>(segmentCount);

        for (int index = 0; index < segmentCount; index++) {
            double routeStartMeters = index * intervalMeters;
            double routeEndMeters = (index == segmentCount - 1)
                    ? effectiveTotalDistanceMeters
                    : Math.min(effectiveTotalDistanceMeters, (index + 1) * intervalMeters);

            double segmentLengthMeters = Math.max(0.0, routeEndMeters - routeStartMeters);
            double segmentDurationSeconds = effectiveTotalDistanceMeters > 0.0
                    ? (segmentLengthMeters / effectiveTotalDistanceMeters) * totalDurationSeconds
                    : (totalDurationSeconds / segmentCount);

            double startDist = routeStartMeters * scaleToPolyline;
            double endDist = (index == segmentCount - 1)
                    ? polylineDistanceMeters
                    : Math.min(polylineDistanceMeters, routeEndMeters * scaleToPolyline);

            List<List<Double>> segmentGeometry = extractSubPolyline(
                    coords,
                    cumulativeDistances,
                    startDist,
                    endDist
            );

            double midDist = (startDist + endDist) / 2.0;
            List<Double> waypointLonLat = interpolateAtDistance(
                    coords,
                    cumulativeDistances,
                    midDist
            );

            List<Double> startPoint = segmentGeometry.getFirst();
            List<Double> endPoint = segmentGeometry.getLast();

            double bearingDegrees = calculateBearingDegrees(
                    startPoint.get(1), startPoint.get(0),
                    endPoint.get(1), endPoint.get(0)
            );

            double fraction = effectiveTotalDistanceMeters > 0.0
                    ? (routeStartMeters / effectiveTotalDistanceMeters)
                    : 0.0;

            long arrivalOffsetSeconds = Math.round(fraction * totalDurationSeconds);
            Instant expectedArrivalTime = snapshot.departureTime()
                    .plusSeconds(arrivalOffsetSeconds);

            int reportedDistanceFromStart = (int) Math.round(routeStartMeters);

            segments.add(new PlannedRouteSegment(
                    index,
                    reportedDistanceFromStart,
                    expectedArrivalTime,
                    waypointLonLat.get(1),
                    waypointLonLat.get(0),
                    bearingDegrees,
                    segmentGeometry,
                    segmentLengthMeters,
                    segmentDurationSeconds
            ));
        }

        return List.copyOf(segments);
    }

    private double resolveTotalDurationSeconds(
            RouteSnapshot snapshot,
            double totalDistanceMeters
    ) {
        if (snapshot.durationSeconds() > 0) {
            return snapshot.durationSeconds();
        }

        double speedMetersPerSecond =
                (snapshot.activityProfile().averageSpeedKmh() * 1000.0) / 3600.0;

        if (speedMetersPerSecond <= 0.0 || totalDistanceMeters <= 0.0) {
            return 0.0;
        }

        return totalDistanceMeters / speedMetersPerSecond;
    }

    private List<List<Double>> extractSubPolyline(
            List<List<Double>> coords,
            double[] cumulativeDistances,
            double startDist,
            double endDist
    ) {
        if (cumulativeDistances[coords.size() - 1] <= 0.0) {
            return List.of(coords.getFirst(), coords.getLast());
        }

        List<List<Double>> result = new ArrayList<>();
        List<Double> startPoint = interpolateAtDistance(
                coords,
                cumulativeDistances,
                startDist
        );
        result.add(startPoint);

        for (int i = 1; i < coords.size() - 1; i++) {
            double d = cumulativeDistances[i];
            if (d > startDist + 1e-3 && d < endDist - 1e-3) {
                result.add(List.of(coords.get(i).get(0), coords.get(i).get(1)));
            }
        }

        List<Double> endPoint = interpolateAtDistance(
                coords,
                cumulativeDistances,
                endDist
        );
        result.add(endPoint);

        return List.copyOf(result);
    }

    private List<Double> interpolateAtDistance(
            List<List<Double>> coords,
            double[] cumulativeDistances,
            double targetDist
    ) {
        int n = coords.size();
        if (targetDist <= 0.0) {
            return List.of(coords.getFirst().get(0), coords.getFirst().get(1));
        }
        if (targetDist >= cumulativeDistances[n - 1]) {
            return List.of(coords.getLast().get(0), coords.getLast().get(1));
        }

        for (int i = 1; i < n; i++) {
            double prevDist = cumulativeDistances[i - 1];
            double nextDist = cumulativeDistances[i];
            if (targetDist <= nextDist) {
                double span = nextDist - prevDist;
                double ratio = span > 0.0 ? (targetDist - prevDist) / span : 0.0;
                List<Double> p0 = coords.get(i - 1);
                List<Double> p1 = coords.get(i);
                double lon = p0.get(0) + ratio * (p1.get(0) - p0.get(0));
                double lat = p0.get(1) + ratio * (p1.get(1) - p0.get(1));
                return List.of(lon, lat);
            }
        }

        return List.of(coords.getLast().get(0), coords.getLast().get(1));
    }

    static double haversineMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_METERS * c;
    }

    static double calculateBearingDegrees(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

        double theta = Math.toDegrees(Math.atan2(y, x));
        return (theta + 360.0) % 360.0;
    }

    public record PlannedRouteSegment(
            int segmentIndex,
            int distanceFromStartMeters,
            Instant expectedArrivalTime,
            double waypointLatitude,
            double waypointLongitude,
            double movementBearingDegrees,
            List<List<Double>> geometryCoordinates,
            double segmentLengthMeters,
            double segmentDurationSeconds
    ) {
        public PlannedRouteSegment(
                int segmentIndex,
                int distanceFromStartMeters,
                Instant expectedArrivalTime,
                double waypointLatitude,
                double waypointLongitude,
                double movementBearingDegrees,
                List<List<Double>> geometryCoordinates
        ) {
            this(
                    segmentIndex,
                    distanceFromStartMeters,
                    expectedArrivalTime,
                    waypointLatitude,
                    waypointLongitude,
                    movementBearingDegrees,
                    geometryCoordinates,
                    0.0,
                    0.0
            );
        }

        public double segmentSpeedKmh(double fallbackSpeedKmh) {
            if (segmentLengthMeters > 0.0 && segmentDurationSeconds > 0.0) {
                return (segmentLengthMeters / segmentDurationSeconds) * 3.6;
            }
            return fallbackSpeedKmh;
        }
    }
}
