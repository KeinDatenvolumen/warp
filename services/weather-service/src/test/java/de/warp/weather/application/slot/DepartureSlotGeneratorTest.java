// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.slot;

import de.warp.weather.application.RouteSegmenter;
import de.warp.weather.application.cache.CacheFreshnessPolicy;
import de.warp.weather.domain.model.RouteSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepartureSlotGeneratorTest {

    private CacheFreshnessPolicy cacheFreshnessPolicy;
    private DepartureSlotGenerator slotGenerator;

    @BeforeEach
    void setUp() {
        cacheFreshnessPolicy = new CacheFreshnessPolicy();
        slotGenerator = new DepartureSlotGenerator(cacheFreshnessPolicy);
    }

    @Test
    void rejectsEmptySegments() {
        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-1", Instant.now(), 3600, 20000,
                List.of(List.of(13.4, 52.5), List.of(13.2, 52.4)),
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );
        assertThatThrownBy(() -> slotGenerator.generateSlotGrid(snapshot, List.of(), null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatesGridWithValidSlotCalculations() {
        Instant now = Instant.parse("2026-05-16T08:00:00Z");
        Instant baseTime = now.plus(Duration.ofHours(2));

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-1", baseTime, 3600, 20000,
                List.of(List.of(13.4, 52.5), List.of(13.2, 52.4)),
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );

        List<RouteSegmenter.PlannedRouteSegment> segments = List.of(
                new RouteSegmenter.PlannedRouteSegment(
                        0, 0, baseTime, 52.5, 13.4, 180.0,
                        List.of(), 10000.0, 1800.0
                ),
                new RouteSegmenter.PlannedRouteSegment(
                        1, 10000, baseTime.plus(Duration.ofMinutes(30)), 52.4, 13.2, 180.0,
                        List.of(), 10000.0, 1800.0
                )
        );

        DepartureSlotGenerator.SlotGridContext context = slotGenerator.generateSlotGrid(
                snapshot, segments, null, null, now
        );

        assertThat(context.numDepartureSlots()).isGreaterThan(0);
        assertThat(context.timeSlotInstants()).isNotEmpty();
        assertThat(context.segmentSlotOffsets()).hasSize(2);
        assertThat(context.segmentWeights()).hasSize(2);
        assertThat(context.fetchPeriod()).isNotNull();
        assertThat(context.locations()).hasSize(2);
    }

    @Test
    void handlesZeroSegmentDurationsAndZeroRouteDuration() {
        Instant now = Instant.parse("2026-05-16T08:00:00Z");
        Instant baseTime = now.plus(Duration.ofHours(24)); // Tomorrow -> candidateStart is after nowCeil

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-zero-dur", baseTime, 0, 20000,
                List.of(List.of(13.4, 52.5), List.of(13.2, 52.4)),
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );

        List<RouteSegmenter.PlannedRouteSegment> segments = List.of(
                new RouteSegmenter.PlannedRouteSegment(
                        0, 0, baseTime, 52.5, 13.4, 180.0,
                        List.of(), 10000.0, 0.0
                )
        );

        DepartureSlotGenerator.SlotGridContext context = slotGenerator.generateSlotGrid(
                snapshot, segments, null, null, now
        );

        assertThat(context.timeSlotInstants()).isNotEmpty();
        assertThat(context.windowStart()).isEqualTo(cacheFreshnessPolicy.normalizeToQuarterHour(baseTime.minus(Duration.ofHours(2))));
    }
}
