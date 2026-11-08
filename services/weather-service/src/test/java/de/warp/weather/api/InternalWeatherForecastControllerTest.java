// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.api;

import de.warp.weather.api.dto.RouteWeatherAiAnalysisRequest;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.api.dto.RouteWeatherOptimizationRequest;
import de.warp.weather.api.dto.RouteWeatherOptimizationResponse;
import de.warp.weather.application.RouteWeatherAiAnalysisService;
import de.warp.weather.application.RouteWeatherOptimizationService;
import de.warp.weather.application.WeatherEvaluationService;
import de.warp.weather.application.WeatherForecastService;
import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherForecastBundle;
import de.warp.weather.domain.model.WeatherForecastLocation;
import de.warp.weather.domain.model.WeatherSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InternalWeatherForecastController Unit Tests")
class InternalWeatherForecastControllerTest {

    @Mock
    private WeatherForecastService weatherForecastService;

    @Mock
    private WeatherEvaluationService weatherEvaluationService;

    @Mock
    private RouteWeatherOptimizationService routeWeatherOptimizationService;

    @Mock
    private RouteWeatherAiAnalysisService routeWeatherAiAnalysisService;

    private InternalWeatherForecastController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalWeatherForecastController(
                weatherForecastService,
                weatherEvaluationService,
                routeWeatherOptimizationService,
                routeWeatherAiAnalysisService
        );
    }

    @Test
    @DisplayName("Alternative constructors instantiate controller successfully")
    void constructorsInstantiateCorrectly() {
        InternalWeatherForecastController singleArg =
                new InternalWeatherForecastController(weatherForecastService);
        assertThat(singleArg).isNotNull();

        InternalWeatherForecastController threeArg =
                new InternalWeatherForecastController(
                        weatherForecastService,
                        weatherEvaluationService,
                        routeWeatherOptimizationService
                );
        assertThat(threeArg).isNotNull();
    }

    @Test
    @DisplayName("getForecast returns WeatherForecastBundle with elevation")
    void getForecastWithElevation() {
        Instant start = Instant.parse("2026-10-22T08:00:00Z");
        Instant end = Instant.parse("2026-10-22T12:00:00Z");
        WeatherForecastBundle expectedBundle = new WeatherForecastBundle(
                Instant.now(),
                List.of()
        );

        when(weatherForecastService.getForecast(
                argThat(locations -> locations.size() == 1
                        && locations.getFirst().id().equals("loc_1")
                        && locations.getFirst().elevationMeters().equals(200.0)),
                eq(new ForecastPeriod(start, end))
        )).thenReturn(expectedBundle);

        WeatherForecastBundle result = controller.getForecast(
                "loc_1", 49.35, 9.15, 200.0, start, end
        );

        assertThat(result).isSameAs(expectedBundle);
    }

    @Test
    @DisplayName("getForecast returns WeatherForecastBundle without elevation")
    void getForecastWithoutElevation() {
        Instant start = Instant.parse("2026-10-22T08:00:00Z");
        Instant end = Instant.parse("2026-10-22T12:00:00Z");
        WeatherForecastBundle expectedBundle = new WeatherForecastBundle(
                Instant.now(),
                List.of()
        );

        when(weatherForecastService.getForecast(
                argThat(locations -> locations.size() == 1
                        && locations.getFirst().id().equals("loc_2")
                        && locations.getFirst().elevationMeters() == null),
                eq(new ForecastPeriod(start, end))
        )).thenReturn(expectedBundle);

        WeatherForecastBundle result = controller.getForecast(
                "loc_2", 49.35, 9.15, null, start, end
        );

        assertThat(result).isSameAs(expectedBundle);
    }

    @Test
    @DisplayName("evaluateRouteWeather throws IllegalArgumentException when request is null")
    void evaluateRouteWeatherThrowsOnNullRequest() {
        assertThatThrownBy(() -> controller.evaluateRouteWeather(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request body must not be null");
    }

    @Test
    @DisplayName("evaluateRouteWeather successfully delegates to service")
    void evaluateRouteWeatherSuccess() {
        Instant now = Instant.now();
        WeatherEvaluation evaluation = new WeatherEvaluation(
                "eval_100",
                "calc_1",
                "usr_1",
                EvaluationStatus.COMPLETED,
                now,
                88,
                "1.0",
                false,
                now,
                now.plusSeconds(3600),
                now,
                List.of()
        );

        EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                "usr_1",
                new EvaluateRouteWeatherRequest.RouteSnapshotRequest(
                        "calc_1",
                        now,
                        3600,
                        25000,
                        List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)),
                        new EvaluateRouteWeatherRequest.ActivityProfileSnapshotRequest(
                                "ROAD_CYCLING",
                                25.0,
                                5
                        )
                )
        );

        when(weatherEvaluationService.evaluateRoute(eq("usr_1"), any(RouteSnapshot.class)))
                .thenReturn(evaluation);

        WeatherEvaluationResponse response = controller.evaluateRouteWeather(request);

        assertThat(response).isNotNull();
        assertThat(response.evaluationId()).isEqualTo("eval_100");
        assertThat(response.overallScore()).isEqualTo(88);
    }

    @Test
    @DisplayName("getEvaluation successfully delegates by ID")
    void getEvaluationSuccess() {
        Instant now = Instant.now();
        WeatherEvaluation evaluation = new WeatherEvaluation(
                "eval_200",
                "calc_2",
                "usr_2",
                EvaluationStatus.COMPLETED,
                now,
                90,
                "1.0",
                false,
                now,
                now.plusSeconds(3600),
                now,
                List.of()
        );

        when(weatherEvaluationService.getEvaluationById("eval_200")).thenReturn(evaluation);

        WeatherEvaluationResponse response = controller.getEvaluation("eval_200");

        assertThat(response).isNotNull();
        assertThat(response.evaluationId()).isEqualTo("eval_200");
        assertThat(response.overallScore()).isEqualTo(90);
    }

    @Test
    @DisplayName("getEvaluationByRoute successfully delegates by route calculation ID")
    void getEvaluationByRouteSuccess() {
        Instant now = Instant.now();
        WeatherEvaluation evaluation = new WeatherEvaluation(
                "eval_300",
                "calc_route_300",
                "usr_3",
                EvaluationStatus.COMPLETED,
                now,
                75,
                "1.0",
                true,
                now,
                now.plusSeconds(3600),
                now,
                List.of()
        );

        when(weatherEvaluationService.getEvaluationByRouteCalculationId("calc_route_300"))
                .thenReturn(evaluation);

        WeatherEvaluationResponse response = controller.getEvaluationByRoute("calc_route_300");

        assertThat(response).isNotNull();
        assertThat(response.routeCalculationId()).isEqualTo("calc_route_300");
        assertThat(response.thunderstormCapApplied()).isTrue();
    }

    @Test
    @DisplayName("optimizeDeparture throws IllegalArgumentException on null request")
    void optimizeDepartureThrowsOnNullRequest() {
        assertThatThrownBy(() -> controller.optimizeDeparture(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request body must not be null");
    }

    @Test
    @DisplayName("optimizeDeparture throws IllegalStateException when service not configured")
    void optimizeDepartureThrowsWhenServiceNotConfigured() {
        InternalWeatherForecastController unconfiguredController =
                new InternalWeatherForecastController(weatherForecastService);

        RouteWeatherOptimizationRequest req = new RouteWeatherOptimizationRequest(
                "usr_1", null, Instant.now(), Instant.now().plusSeconds(3600)
        );

        assertThatThrownBy(() -> unconfiguredController.optimizeDeparture(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Route weather optimization service is not configured");
    }

    @Test
    @DisplayName("optimizeDeparture delegates successfully to service")
    void optimizeDepartureSuccess() {
        RouteWeatherOptimizationRequest req = new RouteWeatherOptimizationRequest(
                "usr_1", null, Instant.now(), Instant.now().plusSeconds(3600)
        );
        RouteWeatherOptimizationResponse expectedResp = new RouteWeatherOptimizationResponse(
                0, null, List.of()
        );

        when(routeWeatherOptimizationService.optimizeDeparture(req)).thenReturn(expectedResp);

        RouteWeatherOptimizationResponse response = controller.optimizeDeparture(req);

        assertThat(response).isSameAs(expectedResp);
        verify(routeWeatherOptimizationService).optimizeDeparture(req);
    }

    @Test
    @DisplayName("analyzeRouteWeatherAi throws IllegalArgumentException on null request")
    void analyzeRouteWeatherAiThrowsOnNullRequest() {
        assertThatThrownBy(() -> controller.analyzeRouteWeatherAi(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request body must not be null");
    }

    @Test
    @DisplayName("analyzeRouteWeatherAi throws IllegalStateException when service not configured")
    void analyzeRouteWeatherAiThrowsWhenServiceNotConfigured() {
        InternalWeatherForecastController unconfiguredController =
                new InternalWeatherForecastController(weatherForecastService);

        RouteWeatherAiAnalysisRequest req = new RouteWeatherAiAnalysisRequest(
                "usr_1", "Mosbach", "Bad Mergentheim", null
        );

        assertThatThrownBy(() -> unconfiguredController.analyzeRouteWeatherAi(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Route weather AI analysis service is not configured");
    }

    @Test
    @DisplayName("analyzeRouteWeatherAi delegates successfully to service")
    void analyzeRouteWeatherAiSuccess() {
        RouteWeatherAiAnalysisRequest req = new RouteWeatherAiAnalysisRequest(
                "usr_1", "Mosbach", "Bad Mergentheim", null
        );
        RouteWeatherAiAnalysisResponse expectedResp = new RouteWeatherAiAnalysisResponse(
                "calc_1", "ROAD_CYCLING", "Rennrad", Instant.now(),
                85, false, "LOW", "Headline", "Summary", "Advice",
                List.of(), List.of(), List.of(), List.of(), "gemini-2.5-flash", Instant.now()
        );

        when(routeWeatherAiAnalysisService.analyzeRouteWeather(req)).thenReturn(expectedResp);

        RouteWeatherAiAnalysisResponse response = controller.analyzeRouteWeatherAi(req);

        assertThat(response).isSameAs(expectedResp);
        verify(routeWeatherAiAnalysisService).analyzeRouteWeather(req);
    }
}
