// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.weather.application;

import de.warp.weather.api.dto.DepartureSlotResultDto;
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

    @Test
    void executesSingleFetchAndFindsOptimalDepartureTime() {
        Instant baseTime = Instant.parse("2026-05-16T08:00:00Z");

        // Simple route from Meißen to Riesa (2 coordinates, ~24 km)
        List<List<Double>> coords = List.of(
                List.of(13.47, 51.16),
                List.of(13.43, 51.20),
                List.of(13.30, 51.30)
        );

        RouteSnapshot snapshot = new RouteSnapshot(
                "calc-123",
                baseTime,
                3600, // 60 min
                24000, // 24 km
                coords,
                new RouteSnapshot.ActivityProfileSnapshot("BICYCLE", 24.0, 10)
        );

        Instant windowStart = Instant.parse("2026-05-16T07:00:00Z");
        Instant windowEnd = Instant.parse("2026-05-16T12:00:00Z");

        // Mock weather response for each location
        when(weatherProvider.getForecast(any(), any())).thenAnswer(invocation -> {
            List<de.warp.weather.domain.model.ForecastLocation> locations =
                    invocation.getArgument(0);
            ForecastPeriod period = invocation.getArgument(1);

            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                List<WeatherForecastTimeStep> steps = new ArrayList<>();
                Instant curr = period.start();
                while (!curr.isAfter(period.end())) {
                    // Say morning (08:00-10:00) is sunny and calm, afternoon has rain/wind
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
                        WeatherModel.BEST_MATCH,
                        loc.latitude(),
                        loc.longitude(),
                        100.0,
                        List.of(),
                        steps
                );

                locs.add(new WeatherForecastLocation(loc, series, Map.of()));
            }

            return new WeatherForecastBundle(Instant.now(), locs);
        });

        RouteWeatherOptimizationResponse response =
                optimizationService.optimizeDeparture(snapshot, windowStart, windowEnd);

        assertThat(response).isNotNull();
        // Verify Single-Fetch: weatherProvider called exactly once
        verify(weatherProvider, times(1)).getForecast(any(), any());

        // Slots should exist
        assertThat(response.departureSlots()).isNotEmpty();
        assertThat(response.recommendedSlotIndex()).isBetween(0, response.departureSlots().size() - 1);

        DepartureSlotResultDto bestSlot = response.departureSlots().get(response.recommendedSlotIndex());
        assertThat(bestSlot.routeScore()).isGreaterThan(70);

        // Morning slots should have higher score than midday rainy slots
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
            List<de.warp.weather.domain.model.ForecastLocation> locations =
                    invocation.getArgument(0);
            ForecastPeriod period = invocation.getArgument(1);

            List<WeatherForecastLocation> locs = new ArrayList<>();
            for (de.warp.weather.domain.model.ForecastLocation loc : locations) {
                List<WeatherForecastTimeStep> steps = new ArrayList<>();
                Instant curr = period.start();
                while (!curr.isAfter(period.end())) {
                    // Severe thunderstorm code 95 with CAPE 1200
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
            // FR-WETT-03: Capped at maximum 50
            assertThat(slot.routeScore()).isLessThanOrEqualTo(50);
        }
    }
}
