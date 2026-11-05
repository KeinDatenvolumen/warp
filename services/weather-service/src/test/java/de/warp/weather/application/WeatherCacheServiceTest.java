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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherCacheServiceTest {

    @Mock
    private WeatherCacheRepository cacheRepository;

    @Mock
    private WeatherProvider weatherProvider;

    private final Instant fixedNow = Instant.parse("2026-10-27T10:00:00Z");
    private WeatherCacheService cacheService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        cacheService = new WeatherCacheService(
                cacheRepository,
                weatherProvider,
                new WeatherCacheProperties(Duration.ofMinutes(60)),
                fixedClock
        );
    }

    @Test
    void buildsDeterministicCacheKeyFromCoordinatesAndForecastHour() {
        Instant arrivalTime = Instant.parse("2026-10-27T12:14:00Z");

        String key1 = WeatherCacheService.buildCacheKey(
                49.35361,
                9.15112,
                arrivalTime,
                WeatherModel.BEST_MATCH
        );
        String key2 = WeatherCacheService.buildCacheKey(
                49.35364,
                9.15114,
                Instant.parse("2026-10-27T12:25:00Z"),
                WeatherModel.BEST_MATCH
        );

        assertThat(key1).isEqualTo("BEST_MATCH:49.3536:9.1511:2026-10-27T12:00:00Z");
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void returnsCachedForecastWithoutCallingProviderWhenEntryIsNotExpired() {
        Instant arrivalTime = Instant.parse("2026-10-27T12:00:00Z");
        String cacheKey = WeatherCacheService.buildCacheKey(
                49.3536,
                9.1511,
                arrivalTime,
                WeatherModel.BEST_MATCH
        );

        WeatherCacheEntity validCachedEntity = new WeatherCacheEntity(
                cacheKey,
                49.3536,
                9.1511,
                arrivalTime,
                WeatherModel.BEST_MATCH,
                new WeatherCondition(12.0, 220.0, 15.0, 0.0, 0.0, 16.5, 1),
                fixedNow.minus(Duration.ofMinutes(30)),
                fixedNow.plus(Duration.ofMinutes(30)) // still valid for 30 minutes
        );

        when(cacheRepository.findByCacheKeyIn(List.of(cacheKey)))
                .thenReturn(List.of(validCachedEntity));

        RouteSegmenter.PlannedRouteSegment segment = new RouteSegmenter.PlannedRouteSegment(
                0,
                0,
                arrivalTime,
                49.3536,
                9.1511,
                90.0,
                List.of(List.of(9.1511, 49.3536), List.of(9.1600, 49.3536))
        );

        List<CachedForecast> results = cacheService.getForecastsForSegments(List.of(segment));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().condition().temperatureCelsius()).isEqualTo(16.5);
        verify(weatherProvider, never()).getForecast(any(), any());
    }

    @Test
    void fetchesFreshDataFromProviderWhenCachedEntryIsOlderThan60Minutes() {
        Instant arrivalTime = Instant.parse("2026-10-27T12:00:00Z");
        String cacheKey = WeatherCacheService.buildCacheKey(
                49.3536,
                9.1511,
                arrivalTime,
                WeatherModel.BEST_MATCH
        );

        // Cached 61 minutes ago, expired 1 minute ago
        WeatherCacheEntity expiredEntity = new WeatherCacheEntity(
                cacheKey,
                49.3536,
                9.1511,
                arrivalTime,
                WeatherModel.BEST_MATCH,
                new WeatherCondition(10.0, 180.0, 10.0, 0.0, 0.0, 11.0, 1),
                fixedNow.minus(Duration.ofMinutes(61)),
                fixedNow.minus(Duration.ofMinutes(1))
        );

        when(cacheRepository.findByCacheKeyIn(List.of(cacheKey)))
                .thenReturn(List.of(expiredEntity));

        ForecastLocation loc = new ForecastLocation("seg_0", 49.3536, 9.1511, null);
        WeatherValues freshValues = new WeatherValues(
                19.5, 19.0, 60.0, 11.0, 14.0,
                20.0, 0.2, 0.2, 0.0, 0.0, 0.0,
                14.0, 250.0, 22.0,
                1015.0, 995.0,
                40.0, 20.0, 20.0, 0.0, 20000.0,
                3.0, 4.0, 300.0, 200.0, 100.0, 1800.0, true,
                150.0, -0.5, -20.0, 2500.0,
                0.8, 0.1, 15.0,
                2
        );
        WeatherForecastSeries series = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH,
                49.3536,
                9.1511,
                180.0,
                List.of(new WeatherForecastTimeStep(arrivalTime, freshValues))
        );
        WeatherForecastBundle bundle = new WeatherForecastBundle(
                fixedNow,
                List.of(new WeatherForecastLocation(loc, series, Map.of()))
        );

        when(weatherProvider.getForecast(any(), any(ForecastPeriod.class)))
                .thenReturn(bundle);

        RouteSegmenter.PlannedRouteSegment segment = new RouteSegmenter.PlannedRouteSegment(
                0,
                0,
                arrivalTime,
                49.3536,
                9.1511,
                90.0,
                List.of(List.of(9.1511, 49.3536), List.of(9.1600, 49.3536))
        );

        List<CachedForecast> results = cacheService.getForecastsForSegments(List.of(segment));

        assertThat(results).hasSize(1);
        CachedForecast fresh = results.getFirst();
        assertThat(fresh.condition().temperatureCelsius()).isEqualTo(19.5);
        assertThat(fresh.condition().windSpeedKmh()).isEqualTo(14.0);
        assertThat(fresh.condition().windDirectionDegrees()).isEqualTo(250.0);
        assertThat(fresh.expiresAt()).isEqualTo(fixedNow.plus(Duration.ofMinutes(60)));

        verify(weatherProvider, times(1)).getForecast(any(), any());
        verify(cacheRepository, times(1)).saveAll(any());
    }
}
