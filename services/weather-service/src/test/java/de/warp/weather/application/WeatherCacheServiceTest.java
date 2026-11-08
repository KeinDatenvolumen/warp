// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void testConstructorsAndNullChecks() {
        WeatherCacheService twoArgService = new WeatherCacheService(cacheRepository, weatherProvider);
        assertThat(twoArgService).isNotNull();

        assertThatThrownBy(() -> WeatherCacheService.normalizeToForecastHour(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> WeatherCacheService.normalizeToQuarterHour(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> WeatherCacheService.buildCacheKey(49.0, 9.0, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalizesForecastHourAndQuarterHourCorrectly() {
        // Under 30 minutes -> current hour
        Instant t1 = Instant.parse("2026-10-27T10:29:59Z");
        assertThat(WeatherCacheService.normalizeToForecastHour(t1))
                .isEqualTo(Instant.parse("2026-10-27T10:00:00Z"));

        // 30 minutes or over -> next hour
        Instant t2 = Instant.parse("2026-10-27T10:30:00Z");
        assertThat(WeatherCacheService.normalizeToForecastHour(t2))
                .isEqualTo(Instant.parse("2026-10-27T11:00:00Z"));

        // Quarter hour rounding
        Instant q1 = Instant.parse("2026-10-27T10:07:00Z");
        assertThat(WeatherCacheService.normalizeToQuarterHour(q1))
                .isEqualTo(Instant.parse("2026-10-27T10:00:00Z"));

        Instant q2 = Instant.parse("2026-10-27T10:08:00Z");
        assertThat(WeatherCacheService.normalizeToQuarterHour(q2))
                .isEqualTo(Instant.parse("2026-10-27T10:15:00Z"));
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
                null // default model fallback
        );

        assertThat(key1).isEqualTo("BEST_MATCH:49.3536:9.1511:2026-10-27T12:00:00Z");
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void returnsEmptyListWhenSegmentsEmpty() {
        List<CachedForecast> result = cacheService.getForecastsForSegments(List.of());
        assertThat(result).isEmpty();
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
                fixedNow.plus(Duration.ofMinutes(30))
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

    @Test
    void testsThunderstormProbabilityDerivationBranches() {
        // Code 99 -> 100.0
        assertThat(WeatherCacheService.deriveThunderstormProbability(99, null, null, 10.0)).isEqualTo(100.0);
        // Code 96 -> 90.0
        assertThat(WeatherCacheService.deriveThunderstormProbability(96, null, null, 10.0)).isEqualTo(90.0);
        // Code 95 -> max(80.0, precipProb)
        assertThat(WeatherCacheService.deriveThunderstormProbability(95, null, null, 70.0)).isEqualTo(80.0);
        assertThat(WeatherCacheService.deriveThunderstormProbability(95, null, null, 88.0)).isEqualTo(88.0);
        // Code 82 -> 55.0
        assertThat(WeatherCacheService.deriveThunderstormProbability(82, null, null, 10.0)).isEqualTo(55.0);

        // CAPE >= 2000.0 -> 85.0
        assertThat(WeatherCacheService.deriveThunderstormProbability(1, 2100.0, 0.0, 10.0)).isEqualTo(85.0);
        // CAPE >= 600.0 and precipProb > 30% -> elevated risk >= 50%
        assertThat(WeatherCacheService.deriveThunderstormProbability(1, 650.0, 0.0, 40.0)).isGreaterThanOrEqualTo(50.0);
        // CAPE >= 600.0 and precipProb <= 30%
        assertThat(WeatherCacheService.deriveThunderstormProbability(1, 650.0, 0.0, 20.0)).isLessThan(50.0);
        // CAPE 100-600
        assertThat(WeatherCacheService.deriveThunderstormProbability(1, 350.0, 0.0, 10.0)).isBetween(10.0, 30.0);

        // Lifted index <= -4.0 adds 15.0
        double base = WeatherCacheService.deriveThunderstormProbability(1, 350.0, 0.0, 10.0);
        double unstable = WeatherCacheService.deriveThunderstormProbability(1, 350.0, -5.0, 10.0);
        assertThat(unstable).isEqualTo(Math.min(100.0, base + 15.0));
    }

    @Test
    void resolvesConditionForEtaWithMinutely15AndHourlySteps() {
        Instant eta = Instant.parse("2026-10-27T12:10:00Z");

        WeatherValues hValues1 = new WeatherValues(
                18.0, 18.0, 60.0, 10.0, 14.0,
                20.0, 0.5, 0.5, 0.0, 0.0, 0.0,
                15.0, 180.0, 20.0, 1013.25, 1013.25,
                20.0, 10.0, 10.0, 0.0, 25000.0,
                3.0, 3.0, 300.0, 200.0, 100.0, 900.0,
                true, 50.0, 0.0, 0.0, 2500.0,
                0.5, 0.2, 18.0, 1
        );
        WeatherValues hValues2 = new WeatherValues(
                20.0, 20.0, 50.0, 9.0, 13.0,
                10.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                12.0, 200.0, 16.0, 1014.0, 1014.0,
                10.0, 0.0, 10.0, 0.0, 30000.0,
                4.0, 4.0, 400.0, 300.0, 100.0, 1800.0,
                true, 40.0, 0.0, 0.0, 2600.0,
                0.6, 0.2, 19.0, 1
        );

        WeatherValues m15Values = new WeatherValues(
                19.0, 19.0, 55.0, 9.5, 13.5,
                15.0, 0.25, 0.25, 0.0, 0.0, 0.0,
                14.0, 190.0, 18.0, 1013.5, 1013.5,
                15.0, 5.0, 10.0, 0.0, 28000.0,
                3.5, 3.5, 350.0, 250.0, 100.0, 1200.0,
                true, 45.0, 0.0, 0.0, 2550.0,
                0.55, 0.2, 18.5, 1
        );

        List<WeatherForecastTimeStep> hourly = List.of(
                new WeatherForecastTimeStep(Instant.parse("2026-10-27T12:00:00Z"), hValues1),
                new WeatherForecastTimeStep(Instant.parse("2026-10-27T13:00:00Z"), hValues2)
        );
        List<WeatherForecastTimeStep> minutely15 = List.of(
                new WeatherForecastTimeStep(Instant.parse("2026-10-27T12:15:00Z"), m15Values)
        );

        WeatherForecastSeries series = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 49.35, 9.15, 100.0, hourly, minutely15
        );

        WeatherCondition cond = cacheService.resolveConditionForEta(series, eta);
        assertThat(cond).isNotNull();
        assertThat(cond.temperatureCelsius()).isEqualTo(19.0);
        // Intensity mm/h is 4 * 0.25 = 1.0
        assertThat(cond.precipitationMm()).isEqualTo(1.0);
        assertThat(cond.wetRoadIndex()).isGreaterThan(0.0);

        // minutely15 with null precipitation falls back to hourly precipitation
        WeatherValues m15NullPrecip = new WeatherValues(
                19.0, 19.0, 55.0, 9.5, 13.5,
                15.0, null, null, null, null, null,
                14.0, 190.0, 18.0, 1013.5, 1013.5,
                15.0, 5.0, 10.0, 0.0, 28000.0,
                3.5, 3.5, 350.0, 250.0, 100.0, 1200.0,
                true, 45.0, 0.0, 0.0, 2550.0,
                0.55, 0.2, 18.5, 1
        );
        WeatherForecastSeries nullPrecipSeries = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 49.35, 9.15, 100.0, hourly,
                List.of(new WeatherForecastTimeStep(Instant.parse("2026-10-27T12:15:00Z"), m15NullPrecip))
        );
        WeatherCondition condFallbackPrecip = cacheService.resolveConditionForEta(nullPrecipSeries, eta);
        assertThat(condFallbackPrecip.precipitationMm()).isNotNull();
    }

    @Test
    void testsToWeatherConditionConversions() {
        WeatherValues values = new WeatherValues(
                18.0, 18.0, 60.0, 10.0, 14.0,
                null, 2.0, 2.0, 0.0, 0.0, 0.0,
                15.0, -10.0, 20.0, 1013.25, 1013.25,
                20.0, 10.0, 10.0, 0.0, 25000.0,
                3.0, 3.0, 300.0, 200.0, 100.0, 900.0,
                true, 50.0, 0.0, 0.0, 2500.0,
                0.5, 0.2, 18.0, 1
        );

        WeatherCondition cond = WeatherCacheService.toWeatherCondition(values);
        assertThat(cond.temperatureCelsius()).isEqualTo(18.0);
        // Negative wind direction normalized to [0, 360)
        assertThat(cond.windDirectionDegrees()).isEqualTo(350.0);
        assertThat(cond.wetRoadIndex()).isGreaterThan(0.0);

        // toWeatherCondition with null fields
        WeatherValues nullish = new WeatherValues(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );
        WeatherCondition condNulls = WeatherCacheService.toWeatherCondition(nullish, 0.2);
        assertThat(condNulls).isNotNull();
        assertThat(condNulls.windSpeedKmh()).isEqualTo(0.0);
        assertThat(condNulls.precipitationMm()).isEqualTo(0.0);
        assertThat(condNulls.precipitationProbabilityPercent()).isEqualTo(0.0);

        // null values throws NPE
        assertThatThrownBy(() -> WeatherCacheService.toWeatherCondition(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolvesConditionForEtaHourlyInterpolationAndFallbacks() {
        WeatherValues hValues1 = new WeatherValues(
                10.0, null, 80.0, null, 8.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                10.0, 350.0, 15.0, 1010.0, 1010.0,
                null, 0.0, 10.0, 0.0, 20000.0,
                null, null, 100.0, 50.0, 50.0, 500.0,
                true, null, 0.0, 0.0, 1500.0,
                null, null, null, 1
        );
        WeatherValues hValues2 = new WeatherValues(
                20.0, 22.0, null, 12.0, null,
                null, 2.0, 2.0, 0.0, 0.0, 0.0,
                20.0, 10.0, 25.0, 1012.0, 1012.0,
                20.0, 5.0, 10.0, 0.0, 30000.0,
                4.0, 4.0, 300.0, 200.0, 100.0, 1000.0,
                true, 50.0, 0.0, 0.0, 2000.0,
                0.5, 0.2, 19.0, 1
        );

        Instant t1 = Instant.parse("2026-10-27T10:00:00Z");
        Instant t2 = Instant.parse("2026-10-27T11:00:00Z");

        List<WeatherForecastTimeStep> hourly = List.of(
                new WeatherForecastTimeStep(t1, hValues1),
                new WeatherForecastTimeStep(t2, hValues2)
        );

        // 1. Series without minutely15 -> purely hourly interpolation
        WeatherForecastSeries hourlyOnlySeries = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 49.35, 9.15, 100.0, hourly, List.of()
        );

        // ETA exactly between t1 and t2 (10:30)
        WeatherCondition cMid = cacheService.resolveConditionForEta(hourlyOnlySeries, Instant.parse("2026-10-27T10:30:00Z"));
        assertThat(cMid).isNotNull();
        assertThat(cMid.temperatureCelsius()).isEqualTo(15.0);
        // Wind direction wrap: 350 and 10 -> around 0 or 360
        assertThat(cMid.windDirectionDegrees()).isNotNull();

        // ETA matching t1 exactly
        WeatherCondition cExact = cacheService.resolveConditionForEta(hourlyOnlySeries, t1);
        assertThat(cExact.temperatureCelsius()).isEqualTo(10.0);

        // ETA before first step
        WeatherCondition cBefore = cacheService.resolveConditionForEta(hourlyOnlySeries, t1.minusSeconds(1800));
        assertThat(cBefore.temperatureCelsius()).isEqualTo(10.0);

        // ETA after last step
        WeatherCondition cAfter = cacheService.resolveConditionForEta(hourlyOnlySeries, t2.plusSeconds(1800));
        assertThat(cAfter.temperatureCelsius()).isEqualTo(20.0);

        // 2. Series with minutely15 step that is > 30 min away from ETA -> falls back to hourly
        List<WeatherForecastTimeStep> farMinutely = List.of(
                new WeatherForecastTimeStep(Instant.parse("2026-10-27T08:00:00Z"), hValues1)
        );
        WeatherForecastSeries farSeries = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 49.35, 9.15, 100.0, hourly, farMinutely
        );
        WeatherCondition cFar = cacheService.resolveConditionForEta(farSeries, Instant.parse("2026-10-27T10:30:00Z"));
        assertThat(cFar.temperatureCelsius()).isEqualTo(15.0);

        // 3. Series with minutely15 step where temperature is null -> falls back to hourly
        List<WeatherForecastTimeStep> nullTempMinutely = List.of(
                new WeatherForecastTimeStep(Instant.parse("2026-10-27T10:30:00Z"), hValues1)
        );
        // Note: hValues1 has temperatureCelsius = 10.0, create one with null temperature
        WeatherValues nullTempValues = new WeatherValues(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );
        WeatherForecastSeries nullTempSeries = new WeatherForecastSeries(
                WeatherModel.BEST_MATCH, 49.35, 9.15, 100.0, hourly,
                List.of(new WeatherForecastTimeStep(Instant.parse("2026-10-27T10:30:00Z"), nullTempValues))
        );
        WeatherCondition cNullTemp = cacheService.resolveConditionForEta(nullTempSeries, Instant.parse("2026-10-27T10:30:00Z"));
        assertThat(cNullTemp.temperatureCelsius()).isEqualTo(15.0);
    }
}
