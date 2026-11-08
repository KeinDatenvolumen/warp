// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-242] (a913a41) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: optimiere Routenplaner, Abfahrtszeit-Slider und gespeicherte Routen (WARP-014)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application;

import de.warp.weather.api.dto.DepartureSlotResultDto;
import de.warp.weather.api.dto.OptimalWindowSummaryDto;
import de.warp.weather.api.dto.RouteWeatherOptimizationRequest;
import de.warp.weather.api.dto.RouteWeatherOptimizationResponse;
import de.warp.weather.application.cache.CacheFreshnessPolicy;
import de.warp.weather.application.hazard.WeatherHazardDetector;
import de.warp.weather.application.port.WeatherProvider;
import de.warp.weather.application.scoring.OptimizationScoringRules;
import de.warp.weather.application.slot.DepartureSlotGenerator;
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
import java.util.Map;
import java.util.Objects;

@Service
public class RouteWeatherOptimizationService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(DISPLAY_ZONE);

    private final RouteSegmenter routeSegmenter;
    private final WeatherProvider weatherProvider;
    private final WeatherScoringService weatherScoringService;

    private final DepartureSlotGenerator departureSlotGenerator;
    private final OptimizationScoringRules scoringRules;
    private final WeatherHazardDetector hazardDetector;
    private final CacheFreshnessPolicy cacheFreshnessPolicy;

    @Autowired
    public RouteWeatherOptimizationService(
            RouteSegmenter routeSegmenter,
            WeatherProvider weatherProvider,
            WeatherScoringService weatherScoringService,
            DepartureSlotGenerator departureSlotGenerator,
            OptimizationScoringRules scoringRules,
            WeatherHazardDetector hazardDetector,
            CacheFreshnessPolicy cacheFreshnessPolicy
    ) {
        this.routeSegmenter = Objects.requireNonNull(routeSegmenter);
        this.weatherProvider = Objects.requireNonNull(weatherProvider);
        this.weatherScoringService = Objects.requireNonNull(weatherScoringService);
        this.departureSlotGenerator = Objects.requireNonNull(departureSlotGenerator);
        this.scoringRules = Objects.requireNonNull(scoringRules);
        this.hazardDetector = Objects.requireNonNull(hazardDetector);
        this.cacheFreshnessPolicy = Objects.requireNonNull(cacheFreshnessPolicy);
    }

    public RouteWeatherOptimizationService(
            RouteSegmenter routeSegmenter,
            WeatherProvider weatherProvider,
            WeatherScoringService weatherScoringService
    ) {
        this(
                routeSegmenter,
                weatherProvider,
                weatherScoringService,
                new DepartureSlotGenerator(new CacheFreshnessPolicy()),
                new OptimizationScoringRules(),
                new WeatherHazardDetector(),
                new CacheFreshnessPolicy()
        );
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

        List<RouteSegmenter.PlannedRouteSegment> plannedSegments = routeSegmenter.segmentRoute(routeSnapshot);
        int numSegments = plannedSegments.size();
        if (numSegments == 0) {
            throw new IllegalArgumentException("Route contains no segments");
        }

        String profileCode = routeSnapshot.activityProfile().code();
        WeatherScoringService.ScoringActivityProfile scoringProfile =
                WeatherScoringService.ScoringActivityProfile.fromCode(profileCode);
        double fallbackSpeedKmh = routeSnapshot.activityProfile().averageSpeedKmh();

        DepartureSlotGenerator.SlotGridContext grid = departureSlotGenerator.generateSlotGrid(
                routeSnapshot,
                plannedSegments,
                requestedWindowStart,
                requestedWindowEnd,
                Instant.now()
        );

        WeatherForecastBundle forecastBundle = weatherProvider.getForecast(grid.locations(), grid.fetchPeriod());
        Map<String, WeatherForecastSeries> seriesByLocation = new HashMap<>();
        if (forecastBundle != null && forecastBundle.locations() != null) {
            for (WeatherForecastLocation loc : forecastBundle.locations()) {
                if (loc.requestedLocation() != null && loc.bestMatch() != null) {
                    seriesByLocation.put(loc.requestedLocation().id(), loc.bestMatch());
                }
            }
        }

        int totalGridSlots = grid.totalGridSlots();
        int[][] segmentScoreMatrix = new int[numSegments][totalGridSlots];
        String[][] segmentReasonMatrix = new String[numSegments][totalGridSlots];
        boolean[][] segmentThunderMatrix = new boolean[numSegments][totalGridSlots];
        boolean[] isDayArray = new boolean[totalGridSlots];

        for (int i = 0; i < numSegments; i++) {
            RouteSegmenter.PlannedRouteSegment planned = plannedSegments.get(i);
            double speedKmh = planned.segmentSpeedKmh(fallbackSpeedKmh);
            WeatherForecastSeries series = seriesByLocation.get("seg_" + i);

            for (int g = 0; g < totalGridSlots; g++) {
                Instant slotTime = grid.timeSlotInstants().get(g);
                WeatherValues values = resolveWeatherValues(series, slotTime);
                double wetRoad = hazardDetector.computeWetRoadIndex(series, slotTime, values);
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

        double tau = scoringRules.resolveProfileTau(scoringProfile);
        int numDepartureSlots = grid.numDepartureSlots();
        List<DepartureSlotResultDto> rawSlots = new ArrayList<>(numDepartureSlots);

        for (int t0 = 0; t0 < numDepartureSlots; t0++) {
            List<Integer> segScores = new ArrayList<>(numSegments);
            List<String> segReasons = new ArrayList<>(numSegments);
            boolean hasThunderCap = false;
            int minSegScore = 100;
            double linearMean = 0.0;

            for (int i = 0; i < numSegments; i++) {
                int slotIdx = Math.min(t0 + grid.segmentSlotOffsets()[i], totalGridSlots - 1);
                int s = segmentScoreMatrix[i][slotIdx];
                segScores.add(s);
                segReasons.add(segmentReasonMatrix[i][slotIdx]);

                if (segmentThunderMatrix[i][slotIdx]) {
                    hasThunderCap = true;
                }
                if (s < minSegScore) {
                    minSegScore = s;
                }
                linearMean += grid.segmentWeights()[i] * s;
            }

            double softMin = scoringRules.calculatePessimisticSoftMinimum(
                    segScores, grid.segmentWeights(), minSegScore, tau
            );
            int finalScore = scoringRules.calculateSlotRouteScore(linearMean, softMin, minSegScore, hasThunderCap);

            int arrivalSlotIdx = Math.min(t0 + grid.totalSlotsOffset(), totalGridSlots - 1);
            boolean reachesNightfall = !isDayArray[arrivalSlotIdx] || !isDayArray[t0];

            Instant depInstant = grid.timeSlotInstants().get(t0);
            Instant arrInstant = grid.timeSlotInstants().get(arrivalSlotIdx);

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

        OptimizationScoringRules.SmoothedSlotsResult smoothedResult =
                scoringRules.applyGaussianSmoothingAndOptimaDetection(rawSlots);

        int bestIdx = smoothedResult.bestSlotIndex();
        int worstIdx = smoothedResult.worstSlotIndex();
        List<DepartureSlotResultDto> processedSlots = smoothedResult.slots();

        DepartureSlotResultDto bestSlot = processedSlots.get(bestIdx);
        DepartureSlotResultDto worstSlot = processedSlots.get(worstIdx);

        String stableWindowStr = scoringRules.calculateStableWindow(processedSlots, bestIdx);
        String advantageExplanation = scoringRules.buildAdvantageExplanation(bestSlot, worstSlot);

        OptimalWindowSummaryDto summary = new OptimalWindowSummaryDto(
                bestSlot.departureTime(),
                bestSlot.departureIso(),
                bestSlot.routeScore(),
                stableWindowStr,
                advantageExplanation,
                worstSlot.departureTime(),
                worstSlot.routeScore()
        );

        for (int m = 0; m < processedSlots.size(); m++) {
            DepartureSlotResultDto slot = processedSlots.get(m);
            String deltaText = scoringRules.buildSlotDeltaExplanation(slot.routeScore(), worstSlot);
            processedSlots.set(m, new DepartureSlotResultDto(
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
                processedSlots
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
        return new CacheFreshnessPolicy().ceilToQuarterHour(timestamp);
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
