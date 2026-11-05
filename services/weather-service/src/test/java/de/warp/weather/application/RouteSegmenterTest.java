// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.application;

import de.warp.weather.domain.model.RouteSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteSegmenterTest {

    private final RouteSegmenter segmenter = new RouteSegmenter();

    @Test
    void segmentsRouteAccordingToActivityProfileIntervalAndComputesArrivalTimes() {
        Instant departureTime = Instant.parse("2026-10-27T10:00:00Z");
        // ~15 km route northwards along longitude 9.15 (from lat 49.35 to 49.485)
        RouteSnapshot snapshot = new RouteSnapshot(
                "calc_123",
                departureTime,
                3000, // 50 minutes total
                15000,
                List.of(
                        List.of(9.1500, 49.3500),
                        List.of(9.1500, 49.4850)
                ),
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 18.0, 5)
        );

        List<RouteSegmenter.PlannedRouteSegment> segments =
                segmenter.segmentRoute(snapshot);

        // 15 km with 5 km interval -> 3 segments
        assertThat(segments).hasSize(3);

        RouteSegmenter.PlannedRouteSegment first = segments.get(0);
        assertThat(first.segmentIndex()).isZero();
        assertThat(first.distanceFromStartMeters()).isZero();
        assertThat(first.expectedArrivalTime()).isEqualTo(departureTime);
        assertThat(first.movementBearingDegrees()).isBetween(0.0, 5.0); // Heading North (~0°)
        assertThat(first.geometryCoordinates()).hasSizeGreaterThanOrEqualTo(2);

        RouteSegmenter.PlannedRouteSegment second = segments.get(1);
        assertThat(second.segmentIndex()).isEqualTo(1);
        assertThat(second.distanceFromStartMeters()).isBetween(4900, 5100);
        assertThat(second.expectedArrivalTime()).isAfter(first.expectedArrivalTime());

        RouteSegmenter.PlannedRouteSegment third = segments.get(2);
        assertThat(third.segmentIndex()).isEqualTo(2);
        assertThat(third.expectedArrivalTime()).isAfter(second.expectedArrivalTime());
    }

    @Test
    void createsDifferentSegmentCountsForHikingBicycleAndMotorcycle() {
        Instant departureTime = Instant.parse("2026-10-27T10:00:00Z");
        List<List<Double>> coordinates = List.of(
                List.of(9.1500, 49.3500),
                List.of(9.1500, 49.4850) // ~15 km
        );

        RouteSnapshot hikingSnapshot = new RouteSnapshot(
                "calc_hiking",
                departureTime,
                10800,
                15000,
                coordinates,
                new RouteSnapshot.ActivityProfileSnapshot("HIKING", 5.0, 2)
        );

        RouteSnapshot motorcycleSnapshot = new RouteSnapshot(
                "calc_moto",
                departureTime,
                900,
                15000,
                coordinates,
                new RouteSnapshot.ActivityProfileSnapshot("MOTORCYCLE", 60.0, 15)
        );

        List<RouteSegmenter.PlannedRouteSegment> hikingSegments =
                segmenter.segmentRoute(hikingSnapshot);
        List<RouteSegmenter.PlannedRouteSegment> motoSegments =
                segmenter.segmentRoute(motorcycleSnapshot);

        // 15 km / 2 km -> 8 segments for hiking; 15 km / 15 km -> 1 segment for motorcycle
        assertThat(hikingSegments).hasSize(8);
        assertThat(motoSegments).hasSize(1);
    }
}
