// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.application;

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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class WeatherCacheService {

    private static final Duration WET_ROAD_LOOKBACK = Duration.ofHours(2);
    private static final int WET_ROAD_INTERVALS = 8;

    private final WeatherCacheRepository cacheRepository;
    private final WeatherProvider weatherProvider;
    private final WeatherCacheProperties cacheProperties;
    private final Clock clock;

    @Autowired
    public WeatherCacheService(
            WeatherCacheRepository cacheRepository,
            WeatherProvider weatherProvider,
            WeatherCacheProperties cacheProperties,
            Clock clock
    ) {
        this.cacheRepository = Objects.requireNonNull(cacheRepository);
        this.weatherProvider = Objects.requireNonNull(weatherProvider);
        this.cacheProperties = Objects.requireNonNull(cacheProperties);
        this.clock = Objects.requireNonNull(clock);
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
        Objects.requireNonNull(forecastTime, "forecastTime must not be null");
        WeatherModel effectiveModel = model == null ? WeatherModel.BEST_MATCH : model;
        Instant normalizedHour = normalizeToForecastHour(forecastTime);

        return String.format(
                Locale.ROOT,
                "%s:%.4f:%.4f:%s",
                effectiveModel.name(),
                roundCoordinate(latitude),
                roundCoordinate(longitude),
                normalizedHour.toString()
        );
    }

    public static Instant normalizeToForecastHour(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Instant truncated = timestamp.truncatedTo(ChronoUnit.HOURS);
        long minutesIntoHour = Duration.between(truncated, timestamp).toMinutes();
        return minutesIntoHour >= 30
                ? truncated.plus(1, ChronoUnit.HOURS)
                : truncated;
    }

    public static Instant normalizeToQuarterHour(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        long epochSeconds = timestamp.getEpochSecond();
        long roundedSeconds = Math.round((double) epochSeconds / 900.0) * 900L;
        return Instant.ofEpochSecond(roundedSeconds);
    }

    private static double roundCoordinate(double coordinate) {
        return Math.round(coordinate * 10_000.0) / 10_000.0;
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
                .map(segment -> buildCacheKey(
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
                .map(s -> normalizeToForecastHour(s.expectedArrivalTime()))
                .min(Comparator.naturalOrder())
                .orElse(now.truncatedTo(ChronoUnit.HOURS));

        Instant maxTime = missingSegments.stream()
                .map(s -> normalizeToForecastHour(s.expectedArrivalTime()))
                .max(Comparator.naturalOrder())
                .orElse(minTime);

        // Request 2 hours earlier so the 8x 15-minute intervals (120 min) for Wet-Road-Memory W_road are included
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
            Instant targetHour = normalizeToForecastHour(eta);

            WeatherCondition condition = resolveConditionForEta(
                    forecastLocation.bestMatch(),
                    eta
            );

            double roundedLat = roundCoordinate(segment.waypointLatitude());
            double roundedLon = roundCoordinate(segment.waypointLongitude());
            String cacheKey = buildCacheKey(
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
                // In minutely_15, precipitation is mm per 15 min -> intensity I_P (mm/h) = 4 * P_15min
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

        double alpha = clamp(
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

        return clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
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
                // Hourly precipitation divided across 4 quarter-hour intervals
                double p15Equivalent = Math.max(0.0, closestHour.values().precipitationMillimeters()) / 4.0;
                pAcc += p15Equivalent * Math.exp(-(0.25 * k) / tauDry);
            }
        }

        return clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
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
        double wetRoadIndex = clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
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
            precipProb = clamp(values.precipitationProbabilityPercent(), 0.0, 100.0);
        } else {
            precipProb = precipMm > 0.0 ? Math.min(100.0, precipMm * 25.0 + 30.0) : 0.0;
        }

        double thunderstormProb = deriveThunderstormProbability(
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
                clamp(wetRoadIndex, 0.0, 1.0),
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
        double probability = 0.0;

        if (weatherCode != null) {
            if (weatherCode == 99) {
                probability = 100.0;
            } else if (weatherCode == 96) {
                probability = 90.0;
            } else if (weatherCode == 95) {
                probability = Math.max(80.0, precipitationProbabilityPercent);
            } else if (weatherCode == 82) {
                probability = 55.0;
            }
        }

        if (cape != null && cape > 100.0) {
            double capeRisk;
            if (cape >= 2000.0) {
                capeRisk = 85.0;
            } else if (cape >= 600.0) {
                // Per [FR-WETT-03]: CAPE >= 600 J/kg with precipProb > 30% triggers elevated thunderstorm risk (>= 50%)
                if (precipitationProbabilityPercent > 30.0) {
                    capeRisk = 50.0 + Math.min(35.0, ((cape - 600.0) / 1400.0) * 35.0);
                } else {
                    capeRisk = 35.0 + ((cape - 600.0) / 1400.0) * 25.0;
                }
            } else {
                capeRisk = ((cape - 100.0) / 500.0) * 30.0;
            }
            probability = Math.max(probability, capeRisk);
        }

        if (liftedIndex != null && liftedIndex <= -4.0) {
            probability = Math.min(100.0, probability + 15.0);
        }

        return clamp(Math.round(probability * 10.0) / 10.0, 0.0, 100.0);
    }

    private static Double lerp(Double a, Double b, double alpha) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return (1.0 - alpha) * a + alpha * b;
    }

    private static Double lerpAngleDegrees(Double deg0, Double deg1, double alpha) {
        if (deg0 == null && deg1 == null) {
            return null;
        }
        if (deg0 == null) {
            return deg1;
        }
        if (deg1 == null) {
            return deg0;
        }
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
