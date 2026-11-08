// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.slot;

import de.warp.weather.application.RouteSegmenter.PlannedRouteSegment;
import de.warp.weather.application.cache.CacheFreshnessPolicy;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.RouteSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class DepartureSlotGenerator {

    private static final Duration WET_ROAD_LOOKBACK = Duration.ofHours(2);
    private final CacheFreshnessPolicy cacheFreshnessPolicy;

    @Autowired
    public DepartureSlotGenerator(CacheFreshnessPolicy cacheFreshnessPolicy) {
        this.cacheFreshnessPolicy = Objects.requireNonNull(cacheFreshnessPolicy);
    }

    public SlotGridContext generateSlotGrid(
            RouteSnapshot routeSnapshot,
            List<PlannedRouteSegment> plannedSegments,
            Instant requestedWindowStart,
            Instant requestedWindowEnd,
            Instant now
    ) {
        Objects.requireNonNull(routeSnapshot, "routeSnapshot must not be null");
        Objects.requireNonNull(plannedSegments, "plannedSegments must not be null");
        int numSegments = plannedSegments.size();
        if (numSegments == 0) {
            throw new IllegalArgumentException("Route contains no segments");
        }

        double totalDurationSec = 0.0;
        double[] segmentDurationsMin = new double[numSegments];
        for (int i = 0; i < numSegments; i++) {
            double durSec = plannedSegments.get(i).segmentDurationSeconds();
            if (durSec <= 0.0) {
                durSec = (routeSnapshot.durationSeconds() > 0 ? routeSnapshot.durationSeconds() : 3600.0) / numSegments;
            }
            totalDurationSec += durSec;
            segmentDurationsMin[i] = durSec / 60.0;
        }

        double totalDurationMin = Math.max(1.0, totalDurationSec / 60.0);
        int totalSlotsOffset = (int) Math.ceil(totalDurationMin / 15.0);

        int[] segmentSlotOffsets = new int[numSegments];
        double[] segmentWeights = new double[numSegments];
        double accMin = 0.0;
        for (int i = 0; i < numSegments; i++) {
            double midSegmentMin = accMin + segmentDurationsMin[i] / 2.0;
            segmentSlotOffsets[i] = (int) Math.round(midSegmentMin / 15.0);
            segmentWeights[i] = segmentDurationsMin[i] / totalDurationMin;
            accMin += segmentDurationsMin[i];
        }

        Instant baseDeparture = routeSnapshot.departureTime() != null ? routeSnapshot.departureTime() : now;
        Instant windowStart;
        if (requestedWindowStart != null) {
            windowStart = cacheFreshnessPolicy.normalizeToQuarterHour(requestedWindowStart);
        } else {
            Instant candidateStart = cacheFreshnessPolicy.normalizeToQuarterHour(baseDeparture.minus(Duration.ofHours(2)));
            Instant nowCeil = cacheFreshnessPolicy.ceilToQuarterHour(now);
            if (baseDeparture.isAfter(now.minus(Duration.ofHours(6))) && candidateStart.isBefore(nowCeil)) {
                windowStart = nowCeil;
            } else {
                windowStart = candidateStart;
            }
        }

        Instant windowEnd = requestedWindowEnd != null
                ? cacheFreshnessPolicy.normalizeToQuarterHour(requestedWindowEnd)
                : cacheFreshnessPolicy.normalizeToQuarterHour(baseDeparture.plus(Duration.ofHours(10)));

        if (!windowEnd.isAfter(windowStart)) {
            windowEnd = windowStart.plus(Duration.ofHours(4));
        }

        if (Duration.between(windowStart, windowEnd).toHours() > 48) {
            windowEnd = windowStart.plus(Duration.ofHours(48));
        }

        int numDepartureSlots = (int) (Duration.between(windowStart, windowEnd).toMinutes() / 15) + 1;
        int totalGridSlots = numDepartureSlots + totalSlotsOffset + 2;

        List<Instant> timeSlotInstants = new ArrayList<>(totalGridSlots);
        for (int g = 0; g < totalGridSlots; g++) {
            timeSlotInstants.add(windowStart.plus(Duration.ofMinutes(15L * g)));
        }

        ForecastPeriod fetchPeriod = new ForecastPeriod(
                windowStart.minus(WET_ROAD_LOOKBACK),
                timeSlotInstants.get(totalGridSlots - 1).plus(Duration.ofHours(1))
        );

        List<ForecastLocation> locations = new ArrayList<>(numSegments);
        for (int i = 0; i < numSegments; i++) {
            PlannedRouteSegment seg = plannedSegments.get(i);
            locations.add(new ForecastLocation("seg_" + i, seg.waypointLatitude(), seg.waypointLongitude(), null));
        }

        return new SlotGridContext(
                windowStart,
                windowEnd,
                numDepartureSlots,
                totalSlotsOffset,
                totalGridSlots,
                segmentSlotOffsets,
                segmentWeights,
                timeSlotInstants,
                fetchPeriod,
                locations
        );
    }

    public record SlotGridContext(
            Instant windowStart,
            Instant windowEnd,
            int numDepartureSlots,
            int totalSlotsOffset,
            int totalGridSlots,
            int[] segmentSlotOffsets,
            double[] segmentWeights,
            List<Instant> timeSlotInstants,
            ForecastPeriod fetchPeriod,
            List<ForecastLocation> locations
    ) {
    }
}
