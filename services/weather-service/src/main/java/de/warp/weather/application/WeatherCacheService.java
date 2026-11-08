// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application;

import de.warp.weather.application.cache.CacheFreshnessPolicy;
import de.warp.weather.application.hazard.WeatherHazardDetector;
import de.warp.weather.application.port.WeatherProvider;
import de.warp.weather.domain.model.CachedForecast;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherForecastBundle;
import de.warp.weather.domain.model.WeatherForecastLocation;
import de.warp.weather.domain.model.WeatherForecastSeries;
import de.warp.weather.domain.model.WeatherForecastTimeStep;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.domain.model.WeatherValues;
import de.warp.weather.infrastructure.persistence.WeatherCacheEntity;
import de.warp.weather.infrastructure.persistence.WeatherCacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class WeatherCacheService {

    private static final Duration WET_ROAD_LOOKBACK = Duration.ofHours(2);
    private static final int WET_ROAD_INTERVALS = 8;

    private static final CacheFreshnessPolicy DEFAULT_POLICY = new CacheFreshnessPolicy();
    private static final WeatherHazardDetector DEFAULT_HAZARD_DETECTOR = new WeatherHazardDetector();

    private final WeatherCacheRepository cacheRepository;
    private final WeatherProvider weatherProvider;
    private final WeatherCacheProperties cacheProperties;
    private final Clock clock;
    private final CacheFreshnessPolicy freshnessPolicy;
    private final WeatherHazardDetector hazardDetector;

    @Autowired
    public WeatherCacheService(
            WeatherCacheRepository cacheRepository,
            WeatherProvider weatherProvider,
            WeatherCacheProperties cacheProperties,
            Clock clock,
            CacheFreshnessPolicy freshnessPolicy,
            WeatherHazardDetector hazardDetector
    ) {
        this.cacheRepository = Objects.requireNonNull(cacheRepository);
        this.weatherProvider = Objects.requireNonNull(weatherProvider);
        this.cacheProperties = Objects.requireNonNull(cacheProperties);
        this.clock = Objects.requireNonNull(clock);
        this.freshnessPolicy = Objects.requireNonNull(freshnessPolicy);
        this.hazardDetector = Objects.requireNonNull(hazardDetector);
    }

    public WeatherCacheService(
            WeatherCacheRepository cacheRepository,
            WeatherProvider weatherProvider,
            WeatherCacheProperties cacheProperties,
            Clock clock
    ) {
        this(
                cacheRepository,
                weatherProvider,
                cacheProperties,
                clock,
                DEFAULT_POLICY,
                DEFAULT_HAZARD_DETECTOR
        );
    }

    public WeatherCacheService(
            WeatherCacheRepository cacheRepository,
            WeatherProvider weatherProvider
    ) {
        this(
                cacheRepository,
                weatherProvider,
                WeatherCacheProperties.defaults(),
                Clock.systemUTC()
        );
    }

    public static String buildCacheKey(
            double latitude,
            double longitude,
            Instant forecastTime,
            WeatherModel model
    ) {
        return DEFAULT_POLICY.buildCacheKey(latitude, longitude, forecastTime, model);
    }

    public static Instant normalizeToForecastHour(Instant timestamp) {
        return DEFAULT_POLICY.normalizeToForecastHour(timestamp);
    }

    public static Instant normalizeToQuarterHour(Instant timestamp) {
        return DEFAULT_POLICY.normalizeToQuarterHour(timestamp);
    }

    @Transactional
    public List<CachedForecast> getForecastsForSegments(
            List<RouteSegmenter.PlannedRouteSegment> segments
    ) {
        Objects.requireNonNull(segments, "segments must not be null");
        if (segments.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now(clock);
        Duration ttl = cacheProperties.ttl();

        List<String> keysBySegment = segments.stream()
                .map(segment -> freshnessPolicy.buildCacheKey(
                        segment.waypointLatitude(),
                        segment.waypointLongitude(),
                        segment.expectedArrivalTime(),
                        WeatherModel.BEST_MATCH
                ))
                .toList();

        Map<String, CachedForecast> validCacheByKey = new LinkedHashMap<>();
        List<WeatherCacheEntity> existingEntities =
                cacheRepository.findByCacheKeyIn(keysBySegment);

        for (WeatherCacheEntity entity : existingEntities) {
            CachedForecast cached = entity.toDomain();
            if (!cached.isExpired(now)) {
                validCacheByKey.put(cached.cacheKey(), cached);
            }
        }

        List<RouteSegmenter.PlannedRouteSegment> missingSegments = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            String key = keysBySegment.get(i);
            if (!validCacheByKey.containsKey(key)) {
                missingSegments.add(segments.get(i));
            }
        }

        if (!missingSegments.isEmpty()) {
            Map<String, CachedForecast> fetchedByKey = fetchAndCacheMissing(
                    missingSegments,
                    now,
                    ttl
            );
            validCacheByKey.putAll(fetchedByKey);
        }

        List<CachedForecast> result = new ArrayList<>(segments.size());
        for (String key : keysBySegment) {
            result.add(validCacheByKey.get(key));
        }

        return List.copyOf(result);
    }

    private Map<String, CachedForecast> fetchAndCacheMissing(
            List<RouteSegmenter.PlannedRouteSegment> missingSegments,
            Instant now,
            Duration ttl
    ) {
        Instant minTime = missingSegments.stream()
                .map(s -> freshnessPolicy.normalizeToForecastHour(s.expectedArrivalTime()))
                .min(Comparator.naturalOrder())
                .orElse(now.truncatedTo(ChronoUnit.HOURS));

        Instant maxTime = missingSegments.stream()
                .map(s -> freshnessPolicy.normalizeToForecastHour(s.expectedArrivalTime()))
                .max(Comparator.naturalOrder())
                .orElse(minTime);

        ForecastPeriod period = new ForecastPeriod(
                minTime.minus(WET_ROAD_LOOKBACK),
                maxTime.plus(1, ChronoUnit.HOURS)
        );

        List<ForecastLocation> locations = new ArrayList<>(missingSegments.size());
        for (RouteSegmenter.PlannedRouteSegment segment : missingSegments) {
            locations.add(new ForecastLocation(
                    "seg_" + segment.segmentIndex(),
                    segment.waypointLatitude(),
                    segment.waypointLongitude(),
                    null
            ));
        }

        WeatherForecastBundle bundle = weatherProvider.getForecast(locations, period);
        Instant cachedAt = bundle.retrievedAt() != null ? bundle.retrievedAt() : now;
        Instant expiresAt = cachedAt.plus(ttl);

        Map<String, CachedForecast> newlyCached = new LinkedHashMap<>();
        List<WeatherCacheEntity> entitiesToSave = new ArrayList<>();

        for (int i = 0; i < missingSegments.size(); i++) {
            RouteSegmenter.PlannedRouteSegment segment = missingSegments.get(i);
            WeatherForecastLocation forecastLocation = bundle.locations().get(i);
            Instant eta = segment.expectedArrivalTime();
            Instant targetHour = freshnessPolicy.normalizeToForecastHour(eta);

            WeatherCondition condition = resolveConditionForEta(
                    forecastLocation.bestMatch(),
                    eta
            );

            double roundedLat = freshnessPolicy.roundCoordinate(segment.waypointLatitude());
            double roundedLon = freshnessPolicy.roundCoordinate(segment.waypointLongitude());
            String cacheKey = freshnessPolicy.buildCacheKey(
                    roundedLat,
                    roundedLon,
                    eta,
                    WeatherModel.BEST_MATCH
            );

            CachedForecast cachedForecast = new CachedForecast(
                    cacheKey,
                    roundedLat,
                    roundedLon,
                    targetHour,
                    cachedAt,
                    expiresAt,
                    WeatherModel.BEST_MATCH,
                    condition
            );

            newlyCached.put(cacheKey, cachedForecast);
            entitiesToSave.add(new WeatherCacheEntity(
                    cacheKey,
                    roundedLat,
                    roundedLon,
                    targetHour,
                    WeatherModel.BEST_MATCH,
                    condition,
                    cachedAt,
                    expiresAt
            ));
        }

        Map<String, WeatherCacheEntity> uniqueEntities = new LinkedHashMap<>();
        for (WeatherCacheEntity entity : entitiesToSave) {
            uniqueEntities.put(entity.getCacheKey(), entity);
        }
        cacheRepository.saveAll(uniqueEntities.values());

        return newlyCached;
    }

    WeatherCondition resolveConditionForEta(
            WeatherForecastSeries series,
            Instant eta
    ) {
        WeatherValues hourlyInterpolated = interpolateHourlyValues(series.hourly(), eta);
        List<WeatherForecastTimeStep> minutely15 = series.minutely15();

        if (minutely15 != null && !minutely15.isEmpty()) {
            WeatherForecastTimeStep closest15m = selectClosestTimeStep(minutely15, eta);
            long diffMinutes = Math.abs(Duration.between(closest15m.time(), eta).toMinutes());
            if (diffMinutes <= 30 && closest15m.values().temperatureCelsius() != null) {
                WeatherValues m15 = closest15m.values();
                Double precipIntensityMmPerHour = m15.precipitationMillimeters() != null
                        ? Math.max(0.0, 4.0 * m15.precipitationMillimeters())
                        : hourlyInterpolated.precipitationMillimeters();

                WeatherValues merged = new WeatherValues(
                        firstNonNull(m15.temperatureCelsius(), hourlyInterpolated.temperatureCelsius()),
                        firstNonNull(m15.apparentTemperatureCelsius(), hourlyInterpolated.apparentTemperatureCelsius()),
                        firstNonNull(m15.relativeHumidityPercent(), hourlyInterpolated.relativeHumidityPercent()),
                        firstNonNull(m15.dewPointCelsius(), hourlyInterpolated.dewPointCelsius()),
                        firstNonNull(m15.wetBulbTemperatureCelsius(), hourlyInterpolated.wetBulbTemperatureCelsius()),
                        firstNonNull(m15.precipitationProbabilityPercent(), hourlyInterpolated.precipitationProbabilityPercent()),
                        precipIntensityMmPerHour,
                        firstNonNull(m15.rainMillimeters(), hourlyInterpolated.rainMillimeters()),
                        firstNonNull(m15.showersMillimeters(), hourlyInterpolated.showersMillimeters()),
                        firstNonNull(m15.snowfallCentimeters(), hourlyInterpolated.snowfallCentimeters()),
                        firstNonNull(m15.snowDepthMeters(), hourlyInterpolated.snowDepthMeters()),
                        firstNonNull(m15.windSpeedKmh(), hourlyInterpolated.windSpeedKmh()),
                        firstNonNull(m15.windDirectionDegrees(), hourlyInterpolated.windDirectionDegrees()),
                        firstNonNull(m15.windGustsKmh(), hourlyInterpolated.windGustsKmh()),
                        firstNonNull(m15.pressureMslHpa(), hourlyInterpolated.pressureMslHpa()),
                        firstNonNull(m15.surfacePressureHpa(), hourlyInterpolated.surfacePressureHpa()),
                        firstNonNull(m15.cloudCoverPercent(), hourlyInterpolated.cloudCoverPercent()),
                        firstNonNull(m15.cloudCoverLowPercent(), hourlyInterpolated.cloudCoverLowPercent()),
                        firstNonNull(m15.cloudCoverMidPercent(), hourlyInterpolated.cloudCoverMidPercent()),
                        firstNonNull(m15.cloudCoverHighPercent(), hourlyInterpolated.cloudCoverHighPercent()),
                        firstNonNull(m15.visibilityMeters(), hourlyInterpolated.visibilityMeters()),
                        firstNonNull(m15.uvIndex(), hourlyInterpolated.uvIndex()),
                        firstNonNull(m15.uvIndexClearSky(), hourlyInterpolated.uvIndexClearSky()),
                        firstNonNull(m15.shortwaveRadiationWattsPerSquareMeter(), hourlyInterpolated.shortwaveRadiationWattsPerSquareMeter()),
                        firstNonNull(m15.directRadiationWattsPerSquareMeter(), hourlyInterpolated.directRadiationWattsPerSquareMeter()),
                        firstNonNull(m15.diffuseRadiationWattsPerSquareMeter(), hourlyInterpolated.diffuseRadiationWattsPerSquareMeter()),
                        firstNonNull(m15.sunshineDurationSeconds(), hourlyInterpolated.sunshineDurationSeconds()),
                        m15.daylight() != null ? m15.daylight() : hourlyInterpolated.daylight(),
                        firstNonNull(m15.capeJoulesPerKilogram(), hourlyInterpolated.capeJoulesPerKilogram()),
                        firstNonNull(m15.liftedIndex(), hourlyInterpolated.liftedIndex()),
                        firstNonNull(m15.convectiveInhibitionJoulesPerKilogram(), hourlyInterpolated.convectiveInhibitionJoulesPerKilogram()),
                        firstNonNull(m15.freezingLevelHeightMeters(), hourlyInterpolated.freezingLevelHeightMeters()),
                        firstNonNull(m15.vapourPressureDeficitKpa(), hourlyInterpolated.vapourPressureDeficitKpa()),
                        firstNonNull(m15.evapotranspirationMillimeters(), hourlyInterpolated.evapotranspirationMillimeters()),
                        firstNonNull(m15.soilTemperatureSurfaceCelsius(), hourlyInterpolated.soilTemperatureSurfaceCelsius()),
                        m15.weatherCode() != null ? m15.weatherCode() : hourlyInterpolated.weatherCode()
                );

                double wetRoadIndex = computeWetRoadIndexFromMinutely15(minutely15, eta, merged);
                return toWeatherCondition(merged, wetRoadIndex);
            }
        }

        double wetRoadIndex = computeWetRoadIndexFromHourly(series.hourly(), eta, hourlyInterpolated);
        return toWeatherCondition(hourlyInterpolated, wetRoadIndex);
    }

    private WeatherValues interpolateHourlyValues(
            List<WeatherForecastTimeStep> steps,
            Instant eta
    ) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalStateException("No hourly time steps available");
        }

        WeatherForecastTimeStep lower = null;
        WeatherForecastTimeStep upper = null;

        for (WeatherForecastTimeStep step : steps) {
            if (!step.time().isAfter(eta)) {
                if (lower == null || step.time().isAfter(lower.time())) {
                    lower = step;
                }
            }
            if (!step.time().isBefore(eta)) {
                if (upper == null || step.time().isBefore(upper.time())) {
                    upper = step;
                }
            }
        }

        if (lower == null) {
            return selectClosestTimeStep(steps, eta).values();
        }
        if (upper == null || lower.time().equals(upper.time())) {
            return lower.values();
        }

        long spanSeconds = Duration.between(lower.time(), upper.time()).toSeconds();
        if (spanSeconds <= 0) {
            return lower.values();
        }

        double alpha = Math.clamp(
                (double) Duration.between(lower.time(), eta).toSeconds() / (double) spanSeconds,
                0.0,
                1.0
        );

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

    private double computeWetRoadIndexFromMinutely15(
            List<WeatherForecastTimeStep> minutely15Steps,
            Instant eta,
            WeatherValues currentValues
    ) {
        double tauDry = WeatherScoringService.calculateDryingTimeConstantHours(
                currentValues.temperatureCelsius() != null ? currentValues.temperatureCelsius() : 18.0,
                currentValues.shortwaveRadiationWattsPerSquareMeter() != null ? currentValues.shortwaveRadiationWattsPerSquareMeter() : 150.0,
                currentValues.relativeHumidityPercent() != null ? currentValues.relativeHumidityPercent() : 60.0
        );

        double pAcc = 0.0;
        for (int k = 0; k <= WET_ROAD_INTERVALS; k++) {
            Instant stepTarget = eta.minus(Duration.ofMinutes(15L * k));
            WeatherForecastTimeStep step = selectClosestTimeStep(minutely15Steps, stepTarget);
            long diffMinutes = Math.abs(Duration.between(step.time(), stepTarget).toMinutes());
            if (diffMinutes <= 15 && step.values().precipitationMillimeters() != null) {
                double p15 = Math.max(0.0, step.values().precipitationMillimeters());
                pAcc += p15 * Math.exp(-(0.25 * k) / tauDry);
            }
        }

        return Math.clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
    }

    private double computeWetRoadIndexFromHourly(
            List<WeatherForecastTimeStep> hourlySteps,
            Instant eta,
            WeatherValues currentValues
    ) {
        double tauDry = WeatherScoringService.calculateDryingTimeConstantHours(
                currentValues.temperatureCelsius() != null ? currentValues.temperatureCelsius() : 18.0,
                currentValues.shortwaveRadiationWattsPerSquareMeter() != null ? currentValues.shortwaveRadiationWattsPerSquareMeter() : 150.0,
                currentValues.relativeHumidityPercent() != null ? currentValues.relativeHumidityPercent() : 60.0
        );

        double pAcc = 0.0;
        for (int k = 0; k <= WET_ROAD_INTERVALS; k++) {
            Instant stepTarget = eta.minus(Duration.ofMinutes(15L * k));
            WeatherForecastTimeStep closestHour = selectClosestTimeStep(hourlySteps, stepTarget);
            long diffMinutes = Math.abs(Duration.between(closestHour.time(), stepTarget).toMinutes());
            if (diffMinutes <= 45 && closestHour.values().precipitationMillimeters() != null) {
                double p15Equivalent = Math.max(0.0, closestHour.values().precipitationMillimeters()) / 4.0;
                pAcc += p15Equivalent * Math.exp(-(0.25 * k) / tauDry);
            }
        }

        return Math.clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
    }

    private WeatherForecastTimeStep selectClosestTimeStep(
            List<WeatherForecastTimeStep> steps,
            Instant targetTime
    ) {
        return steps.stream()
                .min(Comparator.comparingLong(step ->
                        Math.abs(Duration.between(step.time(), targetTime).toSeconds())
                ))
                .orElseThrow(() -> new IllegalStateException("No time steps available"));
    }

    static WeatherCondition toWeatherCondition(WeatherValues values) {
        double temp = values.temperatureCelsius() != null ? values.temperatureCelsius() : 18.0;
        double rad = values.shortwaveRadiationWattsPerSquareMeter() != null
                ? values.shortwaveRadiationWattsPerSquareMeter()
                : 0.0;
        double rh = values.relativeHumidityPercent() != null ? values.relativeHumidityPercent() : 60.0;
        double precipMm = values.precipitationMillimeters() != null
                ? Math.max(0.0, values.precipitationMillimeters())
                : 0.0;
        double tauDry = WeatherScoringService.calculateDryingTimeConstantHours(temp, rad, rh);
        double pAcc = 0.0;
        for (int k = 0; k <= 3; k++) {
            pAcc += (precipMm / 4.0) * Math.exp(-(0.25 * k) / tauDry);
        }
        double wetRoadIndex = Math.clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
        return toWeatherCondition(values, wetRoadIndex);
    }

    static WeatherCondition toWeatherCondition(WeatherValues values, double wetRoadIndex) {
        double windSpeed = values.windSpeedKmh() != null
                ? Math.max(0.0, values.windSpeedKmh())
                : 0.0;

        double windDir = values.windDirectionDegrees() != null
                ? ((values.windDirectionDegrees() % 360.0) + 360.0) % 360.0
                : 0.0;

        Double windGusts = values.windGustsKmh() != null
                ? Math.max(windSpeed, values.windGustsKmh())
                : windSpeed;

        double precipMm = values.precipitationMillimeters() != null
                ? Math.max(0.0, values.precipitationMillimeters())
                : 0.0;

        double precipProb;
        if (values.precipitationProbabilityPercent() != null) {
            precipProb = Math.clamp(values.precipitationProbabilityPercent(), 0.0, 100.0);
        } else {
            precipProb = precipMm > 0.0 ? Math.min(100.0, precipMm * 25.0 + 30.0) : 0.0;
        }

        double thunderstormProb = DEFAULT_HAZARD_DETECTOR.deriveThunderstormProbability(
                values.weatherCode(),
                values.capeJoulesPerKilogram(),
                values.liftedIndex(),
                precipProb
        );

        return new WeatherCondition(
                windSpeed,
                windDir,
                windGusts,
                precipProb,
                precipMm,
                Math.clamp(wetRoadIndex, 0.0, 1.0),
                values.snowfallCentimeters(),
                thunderstormProb,
                values.temperatureCelsius(),
                values.apparentTemperatureCelsius(),
                values.relativeHumidityPercent(),
                values.dewPointCelsius(),
                values.shortwaveRadiationWattsPerSquareMeter(),
                values.soilTemperatureSurfaceCelsius(),
                values.visibilityMeters(),
                values.capeJoulesPerKilogram(),
                values.uvIndex(),
                values.weatherCode()
        );
    }

    static double deriveThunderstormProbability(
            Integer weatherCode,
            Double cape,
            Double liftedIndex,
            double precipitationProbabilityPercent
    ) {
        return DEFAULT_HAZARD_DETECTOR.deriveThunderstormProbability(
                weatherCode, cape, liftedIndex, precipitationProbabilityPercent
        );
    }

    private static Double lerp(Double a, Double b, double alpha) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return (1.0 - alpha) * a + alpha * b;
    }

    private static Double lerpAngleDegrees(Double deg0, Double deg1, double alpha) {
        if (deg0 == null && deg1 == null) return null;
        if (deg0 == null) return deg1;
        if (deg1 == null) return deg0;
        double rad0 = Math.toRadians(deg0);
        double rad1 = Math.toRadians(deg1);
        double x = (1.0 - alpha) * Math.cos(rad0) + alpha * Math.cos(rad1);
        double y = (1.0 - alpha) * Math.sin(rad0) + alpha * Math.sin(rad1);
        double angle = Math.toDegrees(Math.atan2(y, x));
        return ((angle % 360.0) + 360.0) % 360.0;
    }

    private static Double firstNonNull(Double primary, Double fallback) {
        return primary != null ? primary : fallback;
    }
}
