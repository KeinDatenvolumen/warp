package de.warp.weather.application;

import de.warp.weather.application.port.WeatherProvider;
import de.warp.weather.domain.model.CachedForecast;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherForecastBundle;
import de.warp.weather.domain.model.WeatherForecastLocation;
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

        ForecastPeriod period = new ForecastPeriod(
                minTime,
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
            Instant targetHour = normalizeToForecastHour(segment.expectedArrivalTime());

            WeatherValues values = selectClosestTimeStepValues(
                    forecastLocation.bestMatch().hourly(),
                    targetHour
            );

            WeatherCondition condition = toWeatherCondition(values);
            double roundedLat = roundCoordinate(segment.waypointLatitude());
            double roundedLon = roundCoordinate(segment.waypointLongitude());
            String cacheKey = buildCacheKey(
                    roundedLat,
                    roundedLon,
                    targetHour,
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

        // Deduplicate by cacheKey before saving
        Map<String, WeatherCacheEntity> uniqueEntities = new LinkedHashMap<>();
        for (WeatherCacheEntity entity : entitiesToSave) {
            uniqueEntities.put(entity.getCacheKey(), entity);
        }
        cacheRepository.saveAll(uniqueEntities.values());

        return newlyCached;
    }

    private WeatherValues selectClosestTimeStepValues(
            List<WeatherForecastTimeStep> steps,
            Instant targetTime
    ) {
        return steps.stream()
                .min(Comparator.comparingLong(step ->
                        Math.abs(Duration.between(step.time(), targetTime).toSeconds())
                ))
                .map(WeatherForecastTimeStep::values)
                .orElseThrow(() -> new IllegalStateException("No hourly time steps available"));
    }

    static WeatherCondition toWeatherCondition(WeatherValues values) {
        double windSpeed = values.windSpeedKmh() != null
                ? Math.max(0.0, values.windSpeedKmh())
                : 0.0;

        double windDir = values.windDirectionDegrees() != null
                ? ((values.windDirectionDegrees() % 360.0) + 360.0) % 360.0
                : 0.0;

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
                precipProb,
                precipMm,
                thunderstormProb,
                values.temperatureCelsius(),
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
                probability = 45.0;
            }
        }

        if (cape != null && cape > 100.0) {
            double capeRisk;
            if (cape >= 2000.0) {
                capeRisk = 85.0;
            } else if (cape >= 1000.0) {
                capeRisk = 60.0 + ((cape - 1000.0) / 1000.0) * 25.0;
            } else if (cape >= 500.0) {
                capeRisk = 35.0 + ((cape - 500.0) / 500.0) * 25.0;
            } else {
                capeRisk = ((cape - 100.0) / 400.0) * 20.0;
            }
            probability = Math.max(probability, capeRisk);
        }

        if (liftedIndex != null && liftedIndex <= -4.0) {
            probability = Math.min(100.0, probability + 15.0);
        }

        return clamp(Math.round(probability * 10.0) / 10.0, 0.0, 100.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
