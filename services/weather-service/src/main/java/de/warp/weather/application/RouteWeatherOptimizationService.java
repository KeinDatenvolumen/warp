package de.warp.weather.application;

import de.warp.weather.api.dto.DepartureSlotResultDto;
import de.warp.weather.api.dto.OptimalWindowSummaryDto;
import de.warp.weather.api.dto.RouteWeatherOptimizationRequest;
import de.warp.weather.api.dto.RouteWeatherOptimizationResponse;
import de.warp.weather.application.port.WeatherProvider;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherForecastBundle;
import de.warp.weather.domain.model.WeatherForecastLocation;
import de.warp.weather.domain.model.WeatherForecastSeries;
import de.warp.weather.domain.model.WeatherForecastTimeStep;
import de.warp.weather.domain.model.WeatherValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity Gemini 3.8 Flash High): Single-Fetch Optimierung & Gleichstandsregel
@Service
public class RouteWeatherOptimizationService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(DISPLAY_ZONE);

    private static final Duration WET_ROAD_LOOKBACK = Duration.ofHours(2);
    private static final int WET_ROAD_INTERVALS = 8;

    private final RouteSegmenter routeSegmenter;
    private final WeatherProvider weatherProvider;
    private final WeatherScoringService weatherScoringService;

    @Autowired
    public RouteWeatherOptimizationService(
            RouteSegmenter routeSegmenter,
            WeatherProvider weatherProvider,
            WeatherScoringService weatherScoringService
    ) {
        this.routeSegmenter = Objects.requireNonNull(routeSegmenter);
        this.weatherProvider = Objects.requireNonNull(weatherProvider);
        this.weatherScoringService = Objects.requireNonNull(weatherScoringService);
    }

    public RouteWeatherOptimizationResponse optimizeDeparture(
            RouteWeatherOptimizationRequest request
    ) {
        Objects.requireNonNull(request, "request must not be null");
        RouteSnapshot routeSnapshot = request.toDomain();
        return optimizeDeparture(routeSnapshot, request.windowStart(), request.windowEnd());
    }

    public RouteWeatherOptimizationResponse optimizeDeparture(
            RouteSnapshot routeSnapshot,
            Instant requestedWindowStart,
            Instant requestedWindowEnd
    ) {
        Objects.requireNonNull(routeSnapshot, "routeSnapshot must not be null");

        // 1. Segmentation
        List<RouteSegmenter.PlannedRouteSegment> plannedSegments =
                routeSegmenter.segmentRoute(routeSnapshot);
        int numSegments = plannedSegments.size();
        if (numSegments == 0) {
            throw new IllegalArgumentException("Route contains no segments");
        }

        String profileCode = routeSnapshot.activityProfile().code();
        WeatherScoringService.ScoringActivityProfile scoringProfile =
                WeatherScoringService.ScoringActivityProfile.fromCode(profileCode);
        double fallbackSpeedKmh = routeSnapshot.activityProfile().averageSpeedKmh();

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

        // Precompute slot offsets delta_i (in 15-min increments) and segment weights
        int[] segmentSlotOffsets = new int[numSegments];
        double[] segmentWeights = new double[numSegments];
        double accMin = 0.0;
        for (int i = 0; i < numSegments; i++) {
            double midSegmentMin = accMin + segmentDurationsMin[i] / 2.0;
            segmentSlotOffsets[i] = (int) Math.round(midSegmentMin / 15.0);
            segmentWeights[i] = segmentDurationsMin[i] / totalDurationMin;
            accMin += segmentDurationsMin[i];
        }

        // 2. Resolve Window [windowStart, windowEnd]
        Instant baseDeparture = routeSnapshot.departureTime() != null
                ? routeSnapshot.departureTime()
                : Instant.now();
        Instant windowStart;
        if (requestedWindowStart != null) {
            windowStart = WeatherCacheService.normalizeToQuarterHour(requestedWindowStart);
        } else {
            Instant candidateStart = WeatherCacheService.normalizeToQuarterHour(baseDeparture.minus(Duration.ofHours(2)));
            Instant nowCeil = ceilToQuarterHour(Instant.now());
            // Prevent proposing departure slots in the past if baseDeparture is recent or in the future
            if (baseDeparture.isAfter(Instant.now().minus(Duration.ofHours(6))) && candidateStart.isBefore(nowCeil)) {
                windowStart = nowCeil;
            } else {
                windowStart = candidateStart;
            }
        }
        Instant windowEnd = requestedWindowEnd != null
                ? WeatherCacheService.normalizeToQuarterHour(requestedWindowEnd)
                : WeatherCacheService.normalizeToQuarterHour(baseDeparture.plus(Duration.ofHours(10)));

        if (!windowEnd.isAfter(windowStart)) {
            windowEnd = windowStart.plus(Duration.ofHours(4));
        }

        // Limit window to at most 48 hours (192 slots)
        if (Duration.between(windowStart, windowEnd).toHours() > 48) {
            windowEnd = windowStart.plus(Duration.ofHours(48));
        }

        int numDepartureSlots = (int) (Duration.between(windowStart, windowEnd).toMinutes() / 15) + 1;
        int totalGridSlots = numDepartureSlots + totalSlotsOffset + 2;

        List<Instant> timeSlotInstants = new ArrayList<>(totalGridSlots);
        for (int g = 0; g < totalGridSlots; g++) {
            timeSlotInstants.add(windowStart.plus(Duration.ofMinutes(15L * g)));
        }

        // 3. Single-Fetch Open-Meteo Bulk Request for all N segments
        ForecastPeriod fetchPeriod = new ForecastPeriod(
                windowStart.minus(WET_ROAD_LOOKBACK),
                timeSlotInstants.get(totalGridSlots - 1).plus(Duration.ofHours(1))
        );

        List<ForecastLocation> locations = new ArrayList<>(numSegments);
        for (int i = 0; i < numSegments; i++) {
            RouteSegmenter.PlannedRouteSegment seg = plannedSegments.get(i);
            locations.add(new ForecastLocation(
                    "seg_" + i,
                    seg.waypointLatitude(),
                    seg.waypointLongitude(),
                    null
            ));
        }

        WeatherForecastBundle forecastBundle = weatherProvider.getForecast(locations, fetchPeriod);
        Map<String, WeatherForecastSeries> seriesByLocation = new HashMap<>();
        if (forecastBundle != null && forecastBundle.locations() != null) {
            for (WeatherForecastLocation loc : forecastBundle.locations()) {
                if (loc.requestedLocation() != null && loc.bestMatch() != null) {
                    seriesByLocation.put(loc.requestedLocation().id(), loc.bestMatch());
                }
            }
        }

        // 4. Precompute N x T matrices
        int[][] segmentScoreMatrix = new int[numSegments][totalGridSlots];
        String[][] segmentReasonMatrix = new String[numSegments][totalGridSlots];
        boolean[][] segmentThunderMatrix = new boolean[numSegments][totalGridSlots];
        boolean[] isDayArray = new boolean[totalGridSlots];

        for (int i = 0; i < numSegments; i++) {
            RouteSegmenter.PlannedRouteSegment planned = plannedSegments.get(i);
            double speedKmh = planned.segmentSpeedKmh(fallbackSpeedKmh);
            WeatherForecastSeries series = seriesByLocation.get("seg_" + i);

            for (int g = 0; g < totalGridSlots; g++) {
                Instant slotTime = timeSlotInstants.get(g);
                WeatherValues values = resolveWeatherValues(series, slotTime);
                double wetRoad = computeWetRoadIndex(series, slotTime, values);
                WeatherCondition condition = WeatherCacheService.toWeatherCondition(values, wetRoad);

                WeatherScoringService.SegmentScoreBreakdown breakdown =
                        weatherScoringService.evaluateSegmentBreakdown(
                                condition,
                                planned.movementBearingDegrees(),
                                scoringProfile,
                                speedKmh
                        );

                segmentScoreMatrix[i][g] = breakdown.score();
                segmentReasonMatrix[i][g] = breakdown.reasonLabel();
                segmentThunderMatrix[i][g] = weatherScoringService.isElevatedThunderstormRisk(condition);

                if (i == 0 || i == numSegments - 1) {
                    isDayArray[g] = values.daylight() == null || values.daylight();
                }
            }
        }

        // 5. Sliding-Window Trace over M candidate departure times
        double tau = scoringProfile == WeatherScoringService.ScoringActivityProfile.MOTORCYCLE
                ? 12.0
                : scoringProfile == WeatherScoringService.ScoringActivityProfile.BICYCLE ? 14.0 : 15.0;

        List<DepartureSlotResultDto> rawSlots = new ArrayList<>(numDepartureSlots);

        for (int t0 = 0; t0 < numDepartureSlots; t0++) {
            List<Integer> segScores = new ArrayList<>(numSegments);
            List<String> segReasons = new ArrayList<>(numSegments);
            boolean hasThunderCap = false;
            int minSegScore = 100;
            double linearMean = 0.0;

            for (int i = 0; i < numSegments; i++) {
                int slotIdx = Math.min(t0 + segmentSlotOffsets[i], totalGridSlots - 1);
                int s = segmentScoreMatrix[i][slotIdx];
                segScores.add(s);
                segReasons.add(segmentReasonMatrix[i][slotIdx]);

                if (segmentThunderMatrix[i][slotIdx]) {
                    hasThunderCap = true;
                }
                if (s < minSegScore) {
                    minSegScore = s;
                }
                linearMean += segmentWeights[i] * s;
            }

            // Pessimistic Soft-Minimum (LSE shifted by minSegScore for numerical stability)
            double expSum = 0.0;
            for (int i = 0; i < numSegments; i++) {
                int s = segScores.get(i);
                expSum += segmentWeights[i] * Math.exp(-(s - minSegScore) / tau);
            }
            double softMin = minSegScore - tau * Math.log(Math.max(1e-15, expSum));

            double bottleneckCap = minSegScore + 0.45 * Math.sqrt(minSegScore * (100.0 - minSegScore));
            double thunderCap = hasThunderCap ? 50.0 : 100.0;
            double routeRaw = Math.min(bottleneckCap, Math.min(thunderCap, 0.35 * linearMean + 0.65 * softMin));

            // Daylight evaluation (informational indicator for advantage summary)
            int arrivalSlotIdx = Math.min(t0 + totalSlotsOffset, totalGridSlots - 1);
            boolean reachesNightfall = !isDayArray[arrivalSlotIdx] || !isDayArray[t0];

            int finalScore = (int) Math.round(Math.clamp(routeRaw, 0.0, 100.0));
            Instant depInstant = timeSlotInstants.get(t0);
            Instant arrInstant = timeSlotInstants.get(arrivalSlotIdx);

            rawSlots.add(new DepartureSlotResultDto(
                    depInstant.toString(),
                    TIME_FORMATTER.format(depInstant),
                    arrInstant.toString(),
                    TIME_FORMATTER.format(arrInstant),
                    finalScore,
                    0.0,
                    hasThunderCap,
                    reachesNightfall,
                    segScores,
                    segReasons,
                    ""
            ));
        }

        // 6. Gaussian Smoothing [0.20, 0.60, 0.20] & Robust Optimum Detection
        int bestIdx = 0;
        double maxRobust = -1.0;
        int worstIdx = 0;
        int minScore = 101;

        for (int m = 0; m < rawSlots.size(); m++) {
            int prev = rawSlots.get(Math.max(0, m - 1)).routeScore();
            int curr = rawSlots.get(m).routeScore();
            int next = rawSlots.get(Math.min(rawSlots.size() - 1, m + 1)).routeScore();

            double robust = Math.round((0.20 * prev + 0.60 * curr + 0.20 * next) * 10.0) / 10.0;
            DepartureSlotResultDto existing = rawSlots.get(m);
            rawSlots.set(m, new DepartureSlotResultDto(
                    existing.departureIso(),
                    existing.departureTime(),
                    existing.arrivalIso(),
                    existing.arrivalTime(),
                    existing.routeScore(),
                    robust,
                    existing.hasThunderCap(),
                    existing.reachesNightfall(),
                    existing.segmentScores(),
                    existing.segmentReasons(),
                    ""
            ));

            if (robust > maxRobust) {
                maxRobust = robust;
                bestIdx = m;
            }

            if (curr < minScore) {
                minScore = curr;
                worstIdx = m;
            }
        }

        // 7. Stable Window & Summary Text
        DepartureSlotResultDto bestSlot = rawSlots.get(bestIdx);
        DepartureSlotResultDto worstSlot = rawSlots.get(worstIdx);

        int stableStartIdx = bestIdx;
        int stableEndIdx = bestIdx;
        int scoreThreshold = Math.max(50, bestSlot.routeScore() - 6);

        while (stableStartIdx > 0 && rawSlots.get(stableStartIdx - 1).routeScore() >= scoreThreshold) {
            stableStartIdx--;
        }
        while (stableEndIdx < rawSlots.size() - 1 && rawSlots.get(stableEndIdx + 1).routeScore() >= scoreThreshold) {
            stableEndIdx++;
        }

        String stableWindowStr = String.format(
                Locale.ROOT,
                "%s – %s",
                rawSlots.get(stableStartIdx).departureTime(),
                rawSlots.get(stableEndIdx).departureTime()
        );

        int deltaPoints = bestSlot.routeScore() - worstSlot.routeScore();
        String advantageExplanation;
        if (worstSlot.hasThunderCap() && !bestSlot.hasThunderCap()) {
            advantageExplanation = String.format(
                    Locale.GERMAN,
                    "+%d Punkte ggü. %s Uhr (umgeht Gewitterfront)",
                    deltaPoints,
                    worstSlot.departureTime()
            );
        } else if (worstSlot.reachesNightfall() && !bestSlot.reachesNightfall()) {
            advantageExplanation = String.format(
                    Locale.GERMAN,
                    "+%d Punkte ggü. %s Uhr (nutzt volles Tageslicht)",
                    deltaPoints,
                    worstSlot.departureTime()
            );
        } else {
            advantageExplanation = String.format(
                    Locale.GERMAN,
                    "+%d Punkte ggü. %s Uhr (optimale Wetterkonditionen)",
                    deltaPoints,
                    worstSlot.departureTime()
            );
        }

        OptimalWindowSummaryDto summary = new OptimalWindowSummaryDto(
                bestSlot.departureTime(),
                bestSlot.departureIso(),
                bestSlot.routeScore(),
                stableWindowStr,
                advantageExplanation,
                worstSlot.departureTime(),
                worstSlot.routeScore()
        );

        // Assign delta explanation per slot
        for (int m = 0; m < rawSlots.size(); m++) {
            DepartureSlotResultDto slot = rawSlots.get(m);
            int diffToWorst = slot.routeScore() - worstSlot.routeScore();
            String deltaText = diffToWorst > 0
                    ? String.format(Locale.GERMAN, "+%d Pkt. ggü. %s Uhr", diffToWorst, worstSlot.departureTime())
                    : "Tiefstwert im Zeitfenster";
            rawSlots.set(m, new DepartureSlotResultDto(
                    slot.departureIso(),
                    slot.departureTime(),
                    slot.arrivalIso(),
                    slot.arrivalTime(),
                    slot.routeScore(),
                    slot.robustScore(),
                    slot.hasThunderCap(),
                    slot.reachesNightfall(),
                    slot.segmentScores(),
                    slot.segmentReasons(),
                    deltaText
            ));
        }

        return new RouteWeatherOptimizationResponse(
                bestIdx,
                summary,
                rawSlots
        );
    }

    private WeatherValues resolveWeatherValues(WeatherForecastSeries series, Instant targetTime) {
        if (series == null) {
            return fallbackWeatherValues();
        }

        List<WeatherForecastTimeStep> minutely15 = series.minutely15();
        if (minutely15 != null && !minutely15.isEmpty()) {
            WeatherForecastTimeStep closest = selectClosest(minutely15, targetTime);
            if (Math.abs(Duration.between(closest.time(), targetTime).toMinutes()) <= 15) {
                return closest.values();
            }
        }

        List<WeatherForecastTimeStep> hourly = series.hourly();
        if (hourly != null && !hourly.isEmpty()) {
            return interpolateHourly(hourly, targetTime);
        }

        return fallbackWeatherValues();
    }

    private WeatherForecastTimeStep selectClosest(List<WeatherForecastTimeStep> steps, Instant target) {
        return steps.stream()
                .min(Comparator.comparingLong(s -> Math.abs(Duration.between(s.time(), target).toSeconds())))
                .orElseThrow();
    }

    private WeatherValues interpolateHourly(List<WeatherForecastTimeStep> steps, Instant target) {
        WeatherForecastTimeStep lower = null;
        WeatherForecastTimeStep upper = null;

        for (WeatherForecastTimeStep step : steps) {
            if (!step.time().isAfter(target)) {
                if (lower == null || step.time().isAfter(lower.time())) {
                    lower = step;
                }
            }
            if (!step.time().isBefore(target)) {
                if (upper == null || step.time().isBefore(upper.time())) {
                    upper = step;
                }
            }
        }

        if (lower == null) return selectClosest(steps, target).values();
        if (upper == null || lower.time().equals(upper.time())) return lower.values();

        long span = Duration.between(lower.time(), upper.time()).toSeconds();
        if (span <= 0) return lower.values();

        double alpha = Math.clamp((double) Duration.between(lower.time(), target).toSeconds() / (double) span, 0.0, 1.0);
        WeatherValues v0 = lower.values();
        WeatherValues v1 = upper.values();

        return new WeatherValues(
                lerp(v0.temperatureCelsius(), v1.temperatureCelsius(), alpha),
                lerp(v0.apparentTemperatureCelsius(), v1.apparentTemperatureCelsius(), alpha),
                lerp(v0.relativeHumidityPercent(), v1.relativeHumidityPercent(), alpha),
                lerp(v0.dewPointCelsius(), v1.dewPointCelsius(), alpha),
                lerp(v0.wetBulbTemperatureCelsius(), v1.wetBulbTemperatureCelsius(), alpha),
                lerp(v0.precipitationProbabilityPercent(), v1.precipitationProbabilityPercent(), alpha),
                lerp(v0.precipitationMillimeters(), v1.precipitationMillimeters(), alpha),
                lerp(v0.rainMillimeters(), v1.rainMillimeters(), alpha),
                lerp(v0.showersMillimeters(), v1.showersMillimeters(), alpha),
                lerp(v0.snowfallCentimeters(), v1.snowfallCentimeters(), alpha),
                lerp(v0.snowDepthMeters(), v1.snowDepthMeters(), alpha),
                lerp(v0.windSpeedKmh(), v1.windSpeedKmh(), alpha),
                lerpAngleDegrees(v0.windDirectionDegrees(), v1.windDirectionDegrees(), alpha),
                lerp(v0.windGustsKmh(), v1.windGustsKmh(), alpha),
                lerp(v0.pressureMslHpa(), v1.pressureMslHpa(), alpha),
                lerp(v0.surfacePressureHpa(), v1.surfacePressureHpa(), alpha),
                lerp(v0.cloudCoverPercent(), v1.cloudCoverPercent(), alpha),
                lerp(v0.cloudCoverLowPercent(), v1.cloudCoverLowPercent(), alpha),
                lerp(v0.cloudCoverMidPercent(), v1.cloudCoverMidPercent(), alpha),
                lerp(v0.cloudCoverHighPercent(), v1.cloudCoverHighPercent(), alpha),
                lerp(v0.visibilityMeters(), v1.visibilityMeters(), alpha),
                lerp(v0.uvIndex(), v1.uvIndex(), alpha),
                lerp(v0.uvIndexClearSky(), v1.uvIndexClearSky(), alpha),
                lerp(v0.shortwaveRadiationWattsPerSquareMeter(), v1.shortwaveRadiationWattsPerSquareMeter(), alpha),
                lerp(v0.directRadiationWattsPerSquareMeter(), v1.directRadiationWattsPerSquareMeter(), alpha),
                lerp(v0.diffuseRadiationWattsPerSquareMeter(), v1.diffuseRadiationWattsPerSquareMeter(), alpha),
                lerp(v0.sunshineDurationSeconds(), v1.sunshineDurationSeconds(), alpha),
                alpha < 0.5 ? v0.daylight() : v1.daylight(),
                lerp(v0.capeJoulesPerKilogram(), v1.capeJoulesPerKilogram(), alpha),
                lerp(v0.liftedIndex(), v1.liftedIndex(), alpha),
                lerp(v0.convectiveInhibitionJoulesPerKilogram(), v1.convectiveInhibitionJoulesPerKilogram(), alpha),
                lerp(v0.freezingLevelHeightMeters(), v1.freezingLevelHeightMeters(), alpha),
                lerp(v0.vapourPressureDeficitKpa(), v1.vapourPressureDeficitKpa(), alpha),
                lerp(v0.evapotranspirationMillimeters(), v1.evapotranspirationMillimeters(), alpha),
                lerp(v0.soilTemperatureSurfaceCelsius(), v1.soilTemperatureSurfaceCelsius(), alpha),
                alpha < 0.5 ? v0.weatherCode() : v1.weatherCode()
        );
    }

    private double computeWetRoadIndex(WeatherForecastSeries series, Instant slotTime, WeatherValues currentValues) {
        if (series == null) return 0.0;

        double temp = currentValues.temperatureCelsius() != null ? currentValues.temperatureCelsius() : 18.0;
        double rad = currentValues.shortwaveRadiationWattsPerSquareMeter() != null ? currentValues.shortwaveRadiationWattsPerSquareMeter() : 150.0;
        double rh = currentValues.relativeHumidityPercent() != null ? currentValues.relativeHumidityPercent() : 60.0;
        double tauDry = WeatherScoringService.calculateDryingTimeConstantHours(temp, rad, rh);

        List<WeatherForecastTimeStep> steps = series.minutely15() != null && !series.minutely15().isEmpty()
                ? series.minutely15()
                : series.hourly();

        if (steps == null || steps.isEmpty()) return 0.0;

        boolean isHourly = steps == series.hourly();
        double pAcc = 0.0;
        for (int k = 0; k <= WET_ROAD_INTERVALS; k++) {
            Instant target = slotTime.minus(Duration.ofMinutes(15L * k));
            WeatherForecastTimeStep closest = selectClosest(steps, target);
            long diffMin = Math.abs(Duration.between(closest.time(), target).toMinutes());
            if (diffMin <= (isHourly ? 45 : 15) && closest.values().precipitationMillimeters() != null) {
                double p15 = closest.values().precipitationMillimeters();
                if (isHourly) p15 /= 4.0;
                pAcc += Math.max(0.0, p15) * Math.exp(-(0.25 * k) / tauDry);
            }
        }

        return Math.clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
    }

    private static Double lerp(Double a, Double b, double alpha) {
        if (a == null) return b;
        if (b == null) return a;
        return a + alpha * (b - a);
    }

    private static Double lerpAngleDegrees(Double a, Double b, double alpha) {
        if (a == null) return b;
        if (b == null) return a;
        double diff = ((b - a + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        return ((a + alpha * diff) % 360.0 + 360.0) % 360.0;
    }

    public static Instant ceilToQuarterHour(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        long epochSeconds = timestamp.getEpochSecond();
        long ceiledSeconds = ((epochSeconds + 899L) / 900L) * 900L;
        return Instant.ofEpochSecond(ceiledSeconds);
    }

    private static WeatherValues fallbackWeatherValues() {
        return new WeatherValues(
                18.0, 18.0, 60.0, 10.0, 14.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                15.0, 90.0, 20.0, 1013.25, 1013.25,
                20.0, 10.0, 10.0, 0.0, 25000.0,
                3.0, 3.0, 300.0, 200.0, 100.0, 900.0,
                true, 0.0, 0.0, 0.0, 2500.0,
                0.5, 0.2, 18.0, 1
        );
    }
}
