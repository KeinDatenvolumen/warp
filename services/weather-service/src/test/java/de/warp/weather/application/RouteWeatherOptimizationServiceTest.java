// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application;

import de.warp.weather.api.dto.DepartureSlotResultDto;
import de.warp.weather.api.dto.RouteWeatherOptimizationRequest;
import de.warp.weather.api.dto.RouteWeatherOptimizationResponse;
import de.warp.weather.application.port.WeatherProvider;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherForecastBundle;
import de.warp.weather.domain.model.WeatherForecastLocation;
import de.warp.weather.domain.model.WeatherForecastSeries;
import de.warp.weather.domain.model.WeatherForecastTimeStep;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.domain.model.WeatherValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteWeatherOptimizationServiceTest {

    private RouteSegmenter routeSegmenter;
    private WeatherProvider weatherProvider;
    private WeatherScoringService weatherScoringService;
    private RouteWeatherOptimizationService optimizationService;

    @BeforeEach
    void setUp() {
        routeSegmenter = new RouteSegmenter();
        weatherProvider = mock(WeatherProvider.class);
        weatherScoringService = new WeatherScoringService();
        optimizationService = new RouteWeatherOptimizationService(
                routeSegmenter,
                weatherProvider,
                weatherScoringService
        );
    }

    private void mockSimpleWeatherSeries(Instant start, Instant end, boolean isNight) {
        when(weatherProvider.getForecast(any(), any())).thenAnswer(invocation -> {
            List<de.warp.weather.domain.model.ForecastLocation> locations = invocation.getArgument(0);
            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                List<WeatherForecastTimeStep> steps = new ArrayList<>();
                Instant curr = start.minus(Duration.ofHours(3));
                while (!curr.isAfter(end.plus(Duration.ofHours(3)))) {
                    WeatherValues values = new WeatherValues(
                            20.0, 20.0, 50.0, 9.0, 14.0,
                            0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                            10.0, 180.0, 15.0, 1013.0, 1013.0,
                            10.0, 0.0, 10.0, 0.0, 30000.0,
                            3.0, 3.0, 300.0, 200.0, 100.0, 900.0,
                            !isNight, 50.0, 0.0, 0.0, 3000.0,
                            0.5, 0.2, 18.0, 1
                    );
                    steps.add(new WeatherForecastTimeStep(curr, values));
                    curr = curr.plus(Duration.ofMinutes(15));
                }
                WeatherForecastSeries series = new WeatherForecastSeries(
                        WeatherModel.BEST_MATCH, loc.latitude(), loc.longitude(), 100.0, List.of(), steps
                );
                locs.add(new WeatherForecastLocation(loc, series, Map.of()));
            }
            return new WeatherForecastBundle(Instant.now(), locs);
        });
    }

    @Test
    void rejectsNullRequestAndSnapshot() {
        assertThatThrownBy(() -> optimizationService.optimizeDeparture((RouteWeatherOptimizationRequest) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> optimizationService.optimizeDeparture((RouteSnapshot) null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsEmptyRouteSegments() {
        RouteSegmenter mockSegmenter = mock(RouteSegmenter.class);
        RouteWeatherOptimizationService customService = new RouteWeatherOptimizationService(
                mockSegmenter,
                weatherProvider,
                weatherScoringService
        );
        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-empty",
                Instant.now(),
                1800,
                10000,
                List.of(List.of(13.4, 52.5), List.of(13.2, 52.4)),
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );
        when(mockSegmenter.segmentRoute(any())).thenReturn(List.of());

        assertThatThrownBy(() -> customService.optimizeDeparture(snapshot, Instant.now(), Instant.now().plusSeconds(3600)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contains no segments");
    }

    @Test
    void ceilToQuarterHourValidatesInputAndRoundsUp() {
        assertThatThrownBy(() -> RouteWeatherOptimizationService.ceilToQuarterHour(null))
                .isInstanceOf(NullPointerException.class);

        Instant exact = Instant.parse("2026-05-16T08:00:00Z");
        assertThat(RouteWeatherOptimizationService.ceilToQuarterHour(exact)).isEqualTo(exact);

        Instant plusOneSecond = Instant.parse("2026-05-16T08:00:01Z");
        assertThat(RouteWeatherOptimizationService.ceilToQuarterHour(plusOneSecond))
                .isEqualTo(Instant.parse("2026-05-16T08:15:00Z"));
    }

    @Test
    void executesSingleFetchAndFindsOptimalDepartureTime() {
        Instant baseTime = Instant.parse("2026-05-16T08:00:00Z");

        List<List<Double>> coords = List.of(
                List.of(13.47, 51.16),
                List.of(13.43, 51.20),
                List.of(13.30, 51.30)
        );

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-123",
                baseTime,
                3600,
                24000,
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 24.0, 10)
        );

        Instant windowStart = Instant.parse("2026-05-16T07:00:00Z");
        Instant windowEnd = Instant.parse("2026-05-16T12:00:00Z");

        when(weatherProvider.getForecast(any(), any())).thenAnswer(invocation -> {
            List<de.warp.weather.domain.model.ForecastLocation> locations = invocation.getArgument(0);
            ForecastPeriod period = invocation.getArgument(1);

            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                List<WeatherForecastTimeStep> steps = new ArrayList<>();
                Instant curr = period.start();
                while (!curr.isAfter(period.end())) {
                    boolean isMorning = curr.isBefore(Instant.parse("2026-05-16T10:30:00Z"));
                    double rain = isMorning ? 0.0 : 4.0;
                    double wind = isMorning ? 8.0 : 35.0;
                    int weatherCode = isMorning ? 1 : 63;

                    WeatherValues values = new WeatherValues(
                            20.0, 20.0, 55.0, 10.0, 14.0,
                            isMorning ? 5.0 : 80.0, rain, rain, 0.0, 0.0, 0.0,
                            wind, 180.0, wind + 10.0, 1015.0, 1015.0,
                            10.0, 0.0, 10.0, 0.0, 30000.0,
                            4.0, 4.0, 500.0, 400.0, 100.0, 900.0,
                            true, 50.0, 0.0, 0.0, 3000.0,
                            0.8, 0.3, 19.0, weatherCode
                    );
                    steps.add(new WeatherForecastTimeStep(curr, values));
                    curr = curr.plus(Duration.ofMinutes(15));
                }

                WeatherForecastSeries series = new WeatherForecastSeries(
                        WeatherModel.BEST_MATCH, loc.latitude(), loc.longitude(), 100.0, List.of(), steps
                );
                locs.add(new WeatherForecastLocation(loc, series, Map.of()));
            }
            return new WeatherForecastBundle(Instant.now(), locs);
        });

        RouteWeatherOptimizationResponse response =
                optimizationService.optimizeDeparture(snapshot, windowStart, windowEnd);

        assertThat(response).isNotNull();
        verify(weatherProvider, times(1)).getForecast(any(), any());
        assertThat(response.departureSlots()).isNotEmpty();
        assertThat(response.recommendedSlotIndex()).isBetween(0, response.departureSlots().size() - 1);

        DepartureSlotResultDto bestSlot = response.departureSlots().get(response.recommendedSlotIndex());
        assertThat(bestSlot.routeScore()).isGreaterThan(70);

        DepartureSlotResultDto lateSlot = response.departureSlots().getLast();
        assertThat(bestSlot.routeScore()).isGreaterThan(lateSlot.routeScore());

        assertThat(response.optimalWindowSummary()).isNotNull();
        assertThat(response.optimalWindowSummary().bestScore()).isEqualTo(bestSlot.routeScore());
        assertThat(response.optimalWindowSummary().stableWindow()).isNotEmpty();
    }

    @Test
    void appliesThunderstormCapWhenElevatedRiskDetected() {
        Instant baseTime = Instant.parse("2026-05-16T12:00:00Z");

        List<List<Double>> coords = List.of(
                List.of(13.47, 51.16),
                List.of(13.30, 51.30)
        );

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-thunder",
                baseTime,
                1800,
                15000,
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("MOTORCYCLE", 50.0, 10)
        );

        Instant windowStart = Instant.parse("2026-05-16T12:00:00Z");
        Instant windowEnd = Instant.parse("2026-05-16T13:00:00Z");

        when(weatherProvider.getForecast(any(), any())).thenAnswer(invocation -> {
            List<de.warp.weather.domain.model.ForecastLocation> locations = invocation.getArgument(0);
            ForecastPeriod period = invocation.getArgument(1);

            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                List<WeatherForecastTimeStep> steps = new ArrayList<>();
                Instant curr = period.start();
                while (!curr.isAfter(period.end())) {
                    WeatherValues values = new WeatherValues(
                            22.0, 22.0, 80.0, 18.0, 20.0,
                            90.0, 12.0, 12.0, 0.0, 0.0, 0.0,
                            45.0, 90.0, 70.0, 1005.0, 1005.0,
                            100.0, 80.0, 60.0, 40.0, 5000.0,
                            1.0, 1.0, 50.0, 20.0, 30.0, 0.0,
                            true, 1200.0, -3.0, 0.0, 2800.0,
                            0.2, 0.1, 20.0, 95
                    );
                    steps.add(new WeatherForecastTimeStep(curr, values));
                    curr = curr.plus(Duration.ofMinutes(15));
                }

                WeatherForecastSeries series = new WeatherForecastSeries(
                        WeatherModel.BEST_MATCH, loc.latitude(), loc.longitude(), 100.0, List.of(), steps
                );
                locs.add(new WeatherForecastLocation(loc, series, Map.of()));
            }
            return new WeatherForecastBundle(Instant.now(), locs);
        });

        RouteWeatherOptimizationResponse response =
                optimizationService.optimizeDeparture(snapshot, windowStart, windowEnd);

        assertThat(response).isNotNull();
        for (DepartureSlotResultDto slot : response.departureSlots()) {
            assertThat(slot.hasThunderCap()).isTrue();
            assertThat(slot.routeScore()).isLessThanOrEqualTo(50);
        }
    }

    @Test
    void handlesNullWindowParametersAndClampsWindowEnd() {
        Instant baseTime = Instant.now().plus(Duration.ofDays(2));
        List<List<Double>> coords = List.of(List.of(13.47, 51.16), List.of(13.30, 51.30));

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-window",
                baseTime,
                1800,
                10000,
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("HIKING", 5.0, 2)
        );

        mockSimpleWeatherSeries(baseTime.minus(Duration.ofHours(5)), baseTime.plus(Duration.ofHours(60)), false);

        // When windowStart and windowEnd are null
        RouteWeatherOptimizationResponse response1 = optimizationService.optimizeDeparture(snapshot, null, null);
        assertThat(response1.departureSlots()).isNotEmpty();

        // When windowEnd is before windowStart
        RouteWeatherOptimizationResponse response2 = optimizationService.optimizeDeparture(
                snapshot, baseTime, baseTime.minus(Duration.ofHours(1))
        );
        assertThat(response2.departureSlots()).isNotEmpty();

        // When window is greater than 48 hours
        RouteWeatherOptimizationResponse response3 = optimizationService.optimizeDeparture(
                snapshot, baseTime, baseTime.plus(Duration.ofHours(72))
        );
        assertThat(response3.departureSlots()).isNotEmpty();
    }

    @Test
    void handlesNightfallAdvantageExplanation() {
        Instant baseTime = Instant.parse("2026-05-16T18:00:00Z");
        List<List<Double>> coords = List.of(List.of(13.47, 51.16), List.of(13.30, 51.30));

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-night",
                baseTime,
                3600,
                15000,
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );

        Instant windowStart = Instant.parse("2026-05-16T16:00:00Z");
        Instant windowEnd = Instant.parse("2026-05-16T22:00:00Z");

        when(weatherProvider.getForecast(any(), any())).thenAnswer(invocation -> {
            List<de.warp.weather.domain.model.ForecastLocation> locations = invocation.getArgument(0);
            ForecastPeriod period = invocation.getArgument(1);

            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                List<WeatherForecastTimeStep> steps = new ArrayList<>();
                Instant curr = period.start();
                while (!curr.isAfter(period.end())) {
                    boolean isDay = curr.isBefore(Instant.parse("2026-05-16T19:00:00Z"));
                    // Day has good weather (score 90), night has cold / bad weather (score 40)
                    double temp = isDay ? 20.0 : 2.0;
                    WeatherValues values = new WeatherValues(
                            temp, temp, 60.0, 5.0, 10.0,
                            0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                            10.0, 180.0, 15.0, 1015.0, 1015.0,
                            0.0, 0.0, 0.0, 0.0, 30000.0,
                            3.0, 3.0, 300.0, 200.0, 100.0, 900.0,
                            isDay, 0.0, 0.0, 0.0, 2500.0,
                            0.5, 0.2, temp, 1
                    );
                    steps.add(new WeatherForecastTimeStep(curr, values));
                    curr = curr.plus(Duration.ofMinutes(15));
                }
                WeatherForecastSeries series = new WeatherForecastSeries(
                        WeatherModel.BEST_MATCH, loc.latitude(), loc.longitude(), 100.0, List.of(), steps
                );
                locs.add(new WeatherForecastLocation(loc, series, Map.of()));
            }
            return new WeatherForecastBundle(Instant.now(), locs);
        });

        RouteWeatherOptimizationResponse response =
                optimizationService.optimizeDeparture(snapshot, windowStart, windowEnd);

        assertThat(response.optimalWindowSummary().advantageVsWorst()).contains("Tageslicht");
    }

    @Test
    void handlesHourlyInterpolationAndWetRoadCalculationFromHourlySteps() {
        Instant baseTime = Instant.parse("2026-05-16T08:00:00Z");
        List<List<Double>> coords = List.of(List.of(13.47, 51.16), List.of(13.30, 51.30));

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-hourly",
                baseTime,
                1800,
                10000,
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );

        when(weatherProvider.getForecast(any(), any())).thenAnswer(invocation -> {
            List<de.warp.weather.domain.model.ForecastLocation> locations = invocation.getArgument(0);
            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                List<WeatherForecastTimeStep> hourlySteps = List.of(
                        new WeatherForecastTimeStep(Instant.parse("2026-05-16T06:00:00Z"), new WeatherValues(
                                15.0, 15.0, 70.0, 10.0, 12.0, 20.0, 2.0, 2.0, 0.0, 0.0, 0.0,
                                12.0, 10.0, 15.0, 1012.0, 1012.0, 40.0, 10.0, 10.0, 0.0, 20000.0,
                                2.0, 2.0, 200.0, 100.0, 100.0, 600.0, true, 0.0, 0.0, 0.0, 2000.0,
                                0.3, 0.1, 14.0, 61
                        )),
                        new WeatherForecastTimeStep(Instant.parse("2026-05-16T09:00:00Z"), new WeatherValues(
                                22.0, 22.0, 50.0, 11.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                8.0, 350.0, 10.0, 1015.0, 1015.0, 10.0, 0.0, 10.0, 0.0, 30000.0,
                                4.0, 4.0, 500.0, 400.0, 100.0, 900.0, true, 0.0, 0.0, 0.0, 2500.0,
                                0.6, 0.2, 19.0, 1
                        )),
                        new WeatherForecastTimeStep(Instant.parse("2026-05-16T12:00:00Z"), new WeatherValues(
                                24.0, 24.0, 45.0, 12.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                10.0, 20.0, 12.0, 1016.0, 1016.0, 10.0, 0.0, 10.0, 0.0, 35000.0,
                                5.0, 5.0, 600.0, 500.0, 100.0, 900.0, true, 0.0, 0.0, 0.0, 2800.0,
                                0.7, 0.3, 21.0, 1
                        ))
                );
                // minutely15 is null/empty -> forces hourly interpolation
                WeatherForecastSeries series = new WeatherForecastSeries(
                        WeatherModel.BEST_MATCH, loc.latitude(), loc.longitude(), 100.0, hourlySteps, List.of()
                );
                locs.add(new WeatherForecastLocation(loc, series, Map.of()));
            }
            return new WeatherForecastBundle(Instant.now(), locs);
        });

        RouteWeatherOptimizationResponse response = optimizationService.optimizeDeparture(
                snapshot, Instant.parse("2026-05-16T07:00:00Z"), Instant.parse("2026-05-16T10:00:00Z")
        );

        assertThat(response.departureSlots()).isNotEmpty();
    }

    @Test
    void handlesNullForecastBundleAndMissingLocationSeriesGracefully() {
        Instant baseTime = Instant.parse("2026-05-16T08:00:00Z");
        List<List<Double>> coords = List.of(List.of(13.47, 51.16), List.of(13.30, 51.30));

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-null-bundle",
                baseTime,
                1800,
                10000,
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 20.0, 5)
        );

        when(weatherProvider.getForecast(any(), any())).thenReturn(null);

        RouteWeatherOptimizationResponse response = optimizationService.optimizeDeparture(
                snapshot, baseTime, baseTime.plus(Duration.ofHours(2))
        );

        assertThat(response).isNotNull();
        assertThat(response.departureSlots()).isNotEmpty();

        // Also test bundle with empty locations
        when(weatherProvider.getForecast(any(), any())).thenReturn(
                new WeatherForecastBundle(Instant.now(), List.of())
        );
        RouteWeatherOptimizationResponse response2 = optimizationService.optimizeDeparture(
                snapshot, baseTime, baseTime.plus(Duration.ofHours(2))
        );
        assertThat(response2).isNotNull();
    }

    @Test
    void handlesHourlyInterpolationEdgeCasesAndNullLerpBranches() {
        Instant baseTime = Instant.parse("2026-05-16T08:00:00Z");
        List<List<Double>> coords = List.of(List.of(13.47, 51.16), List.of(13.30, 51.30));

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-hourly-edge",
                baseTime,
                1800,
                10000,
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("MOTORCYCLE", 60.0, 5)
        );

        when(weatherProvider.getForecast(any(), any())).thenAnswer(invocation -> {
            List<de.warp.weather.domain.model.ForecastLocation> locations = invocation.getArgument(0);
            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                // Hourly steps where first step has null values in some fields, second step has null in others
                List<WeatherForecastTimeStep> hourlySteps = List.of(
                        new WeatherForecastTimeStep(Instant.parse("2026-05-16T08:00:00Z"), new WeatherValues(
                                null, 15.0, null, null, null, null, null, null, null, null, null,
                                null, 350.0, null, null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, null, null, null, null,
                                null, null, null, 1
                        )),
                        new WeatherForecastTimeStep(Instant.parse("2026-05-16T09:00:00Z"), new WeatherValues(
                                20.0, null, 50.0, 10.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                10.0, null, 12.0, 1013.0, 1013.0, 10.0, 0.0, 10.0, 0.0, 20000.0,
                                3.0, 3.0, 200.0, 100.0, 100.0, 500.0, true, 0.0, 0.0, 0.0, 2000.0,
                                0.5, 0.2, 18.0, 1
                        ))
                );
                WeatherForecastSeries series = new WeatherForecastSeries(
                        WeatherModel.BEST_MATCH, loc.latitude(), loc.longitude(), 100.0, hourlySteps, List.of()
                );
                locs.add(new WeatherForecastLocation(loc, series, Map.of()));
            }
            return new WeatherForecastBundle(Instant.now(), locs);
        });

        RouteWeatherOptimizationResponse response = optimizationService.optimizeDeparture(
                snapshot, Instant.parse("2026-05-16T07:00:00Z"), Instant.parse("2026-05-16T10:00:00Z")
        );

        assertThat(response.departureSlots()).isNotEmpty();
    }
}
