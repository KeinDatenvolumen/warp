// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application;

import de.warp.weather.api.EvaluateRouteWeatherRequest.ActivityProfileSnapshotRequest;
import de.warp.weather.api.EvaluateRouteWeatherRequest.RouteSnapshotRequest;
import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisRequest;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.domain.model.CachedForecast;
import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.domain.model.RouteSnapshot;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.domain.model.WeatherSegment;
import de.warp.weather.infrastructure.gemini.GeminiClient;
import de.warp.weather.infrastructure.gemini.GeminiClient.GeminiStructuredOutput;
import de.warp.weather.infrastructure.mcp.McpToolRegistry;
import de.warp.weather.infrastructure.mcp.WeatherMcpTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteWeatherAiAnalysisServiceTest {

    private RouteSegmenter routeSegmenter;
    private WeatherScoringService weatherScoringService;
    private WeatherMcpTools weatherMcpTools;
    private McpToolRegistry mcpToolRegistry;

    @Mock
    private WeatherCacheService weatherCacheService;

    @Mock
    private GeminiClient geminiClient;

    private ObjectMapper objectMapper;
    private Clock fixedClock;
    private Instant fixedNow;
    private RouteWeatherAiAnalysisService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fixedNow = Instant.parse("2026-05-16T08:00:00Z");
        fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        routeSegmenter = new RouteSegmenter();
        weatherScoringService = new WeatherScoringService();
        weatherMcpTools = new WeatherMcpTools(weatherScoringService, null);
        mcpToolRegistry = new McpToolRegistry(weatherMcpTools, objectMapper);

        service = new RouteWeatherAiAnalysisService(
                routeSegmenter,
                weatherCacheService,
                weatherScoringService,
                weatherMcpTools,
                mcpToolRegistry,
                geminiClient,
                objectMapper,
                fixedClock
        );
    }

    private RouteWeatherAiAnalysisRequest createSampleRequest(String profileCode, String calcId) {
        return new RouteWeatherAiAnalysisRequest(
                "user-1",
                "Berlin",
                "Potsdam",
                new RouteSnapshotRequest(
                        calcId,
                        fixedNow.plus(Duration.ofHours(1)),
                        3600,
                        30000,
                        List.of(
                                List.of(13.405, 52.520),
                                List.of(13.200, 52.450),
                                List.of(13.060, 52.390)
                        ),
                        new ActivityProfileSnapshotRequest(profileCode, 25.0, 10)
                )
        );
    }

    private List<CachedForecast> createSampleForecasts(List<RouteSegmenter.PlannedRouteSegment> planned) {
        return planned.stream().map(s -> new CachedForecast(
                "key_" + s.segmentIndex(),
                s.waypointLatitude(),
                s.waypointLongitude(),
                s.expectedArrivalTime(),
                fixedNow,
                fixedNow.plusSeconds(3600),
                WeatherModel.BEST_MATCH,
                new WeatherCondition(
                        15.0,
                        180.0,
                        25.0,
                        20.0,
                        0.0,
                        0.1,
                        0.0,
                        10.0,
                        18.0,
                        18.0,
                        55.0,
                        9.0,
                        400.0,
                        16.0,
                        25000.0,
                        150.0,
                        3.0,
                        1
                )
        )).toList();
    }

    @Test
    void rejectsNullRequest() {
        assertThatThrownBy(() -> service.analyzeRouteWeather(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void returnsCachedAnalysisOnSecondCallWithinTtl() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-cache-1");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(inv -> {
            List<RouteSegmenter.PlannedRouteSegment> planned = inv.getArgument(0);
            return createSampleForecasts(planned);
        });
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse resp1 = service.analyzeRouteWeather(request);
        RouteWeatherAiAnalysisResponse resp2 = service.analyzeRouteWeather(request);

        assertThat(resp1).isSameAs(resp2);
        verify(weatherCacheService, times(1)).getForecastsForSegments(anyList());
    }

    @Test
    void usesExistingEvaluationWhenCloseInDepartureTime() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-existing-1");
        Instant departure = fixedNow.plus(Duration.ofHours(1));
        WeatherEvaluation existing = new WeatherEvaluation(
                "wev-1",
                "calc-existing-1",
                "user-1",
                EvaluationStatus.COMPLETED,
                departure.plus(Duration.ofMinutes(5)), // within 15 min
                88,
                "1.0",
                false,
                fixedNow,
                fixedNow.plusSeconds(3600),
                fixedNow,
                List.of(new WeatherSegment(
                        0,
                        departure,
                        88,
                        "#00FF00",
                        0,
                        new WeatherCondition(10.0, 180.0, 15.0, 0.0, 0.0, 18.0, 1),
                        List.of(),
                        "NONE",
                        "Optimal"
                ))
        );

        weatherMcpTools.registerEphemeralEvaluation("calc-existing-1", existing);
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.overallScore()).isEqualTo(88);
        verify(weatherCacheService, never()).getForecastsForSegments(any());
    }

    @Test
    void fallsBackToPersistedEvaluationWhenWeatherCacheServiceThrows() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-fallback-1");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(inv -> {
            WeatherEvaluation fallbackEval = new WeatherEvaluation(
                    "wev-fb",
                    "calc-fallback-1",
                    "user-1",
                    EvaluationStatus.COMPLETED,
                    fixedNow,
                    75,
                    "1.0",
                    false,
                    fixedNow,
                    fixedNow.plusSeconds(3600),
                    fixedNow,
                    List.of(new WeatherSegment(
                            0,
                            fixedNow,
                            75,
                            "#AABBCC",
                            0,
                            new WeatherCondition(15.0, 180.0, 20.0, 0.0, 0.0, 18.0, 1),
                            List.of(),
                            "WIND",
                            "Windig"
                    ))
            );
            weatherMcpTools.registerEphemeralEvaluation("calc-fallback-1", fallbackEval);
            throw new RuntimeException("API down");
        });
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.overallScore()).isEqualTo(75);
    }

    @Test
    void rethrowsExceptionWhenForecastProviderFailsAndNoPersistedEvaluationExists() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-no-fallback");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenThrow(new RuntimeException("API unreachable"));

        assertThatThrownBy(() -> service.analyzeRouteWeather(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API unreachable");
    }

    @Test
    void callsGeminiAndMapsStructuredOutputCorrectly() throws Exception {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("MOTORCYCLE", "calc-gemini-ok");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(inv -> {
            List<RouteSegmenter.PlannedRouteSegment> planned = inv.getArgument(0);
            return createSampleForecasts(planned);
        });
        when(geminiClient.isConfigured()).thenReturn(true);

        String json = """
                {
                  "riskLevel": "MODERATE",
                  "headline": "Gemini Analyse Motorrad",
                  "overallSummary": "Gute Bedingungen mit leichtem Wind.",
                  "activityProfileAdvice": "Helmvisier geschlossen halten.",
                  "hiddenParameterInsights": [
                    {
                      "parameterKey": "WET_ROAD_INDEX",
                      "parameterLabel": "Nässe",
                      "category": "ROAD_SURFACE",
                      "observedValue": "15 %",
                      "status": "OPTIMAL",
                      "interpretation": "Trockene Fahrbahn"
                    }
                  ],
                  "criticalSections": [
                    {
                      "segmentIndexStart": 0,
                      "segmentIndexEnd": 1,
                      "distanceRangeKm": "km 0 - 15",
                      "timeWindow": "09:00 - 09:20",
                      "minSegmentScore": 65,
                      "severity": "WARNING",
                      "primaryFactor": "WIND",
                      "title": "Achtung Wind",
                      "description": "Böen bis 35 km/h",
                      "recommendation": "Spur halten"
                    }
                  ],
                  "recommendations": [
                    "Tempo drosseln",
                    "Schutzkleidung tragen"
                  ]
                }
                """;
        JsonNode jsonNode = objectMapper.readTree(json);
        when(geminiClient.generateStructuredAnalysis(anyString(), anyString(), anyMap()))
                .thenReturn(new GeminiStructuredOutput(jsonNode, "gemini-2.5-pro"));

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.riskLevel()).isEqualTo("MODERATE");
        assertThat(response.headline()).isEqualTo("Gemini Analyse Motorrad");
        assertThat(response.overallSummary()).isEqualTo("Gute Bedingungen mit leichtem Wind.");
        assertThat(response.activityProfileAdvice()).isEqualTo("Helmvisier geschlossen halten.");
        assertThat(response.modelName()).isEqualTo("gemini-2.5-pro (MCP)");
        assertThat(response.recommendations()).containsExactly("Tempo drosseln", "Schutzkleidung tragen");
        assertThat(response.hiddenParameterInsights()).hasSize(1);
        assertThat(response.criticalSections()).hasSize(1);
    }

    @Test
    void fallsBackToPhysicsSynthesisWhenGeminiThrowsException() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("HIKING", "calc-gemini-fail");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(inv -> {
            List<RouteSegmenter.PlannedRouteSegment> planned = inv.getArgument(0);
            return planned.stream().map(s -> new CachedForecast(
                    "key_" + s.segmentIndex(),
                    s.waypointLatitude(),
                    s.waypointLongitude(),
                    s.expectedArrivalTime(),
                    fixedNow,
                    fixedNow.plusSeconds(3600),
                    WeatherModel.BEST_MATCH,
                    new WeatherCondition(
                            25.0, 180.0, 50.0, 95.0, 10.0, 0.8, 0.0, 90.0,
                            22.0, 22.0, 85.0, 19.0, 100.0, 20.0, 5000.0, 1200.0, 2.0, 95
                    )
            )).toList();
        });
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateStructuredAnalysis(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Gemini quota exceeded"));

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.riskLevel()).isEqualTo("CRITICAL");
        assertThat(response.modelName()).isEqualTo("warp-mcp-physics-engine (Fallback)");
        assertThat(response.thunderstormCapApplied()).isTrue();
        assertThat(response.headline()).contains("Gewitterrisiko");
        assertThat(response.recommendations()).anyMatch(r -> r.contains("Gewitterfenster"));
    }

    @Test
    void fallsBackToPhysicsEngineWhenGeminiExceedsTimeout() {
        service.setAiRequestTimeout(Duration.ofMillis(100));
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-gemini-timeout");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(inv -> {
            List<RouteSegmenter.PlannedRouteSegment> planned = inv.getArgument(0);
            return createSampleForecasts(planned);
        });
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateStructuredAnalysis(anyString(), anyString(), anyMap()))
                .thenAnswer(inv -> {
                    Thread.sleep(500);
                    return null;
                });

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.modelName()).isEqualTo("warp-mcp-physics-engine (Fallback)");
        assertThat(response.hiddenParameterInsights()).isNotEmpty();
    }

    @Test
    void testsAllDeterministicHiddenInsightsBranches() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-deterministic");

        Instant t0 = fixedNow.plus(Duration.ofHours(1));
        List<CachedForecast> extremeForecasts = List.of(
                new CachedForecast(
                        "key_0", 52.52, 13.40, t0, fixedNow, fixedNow.plusSeconds(3600), WeatherModel.BEST_MATCH,
                        new WeatherCondition(
                                40.0, 180.0, 50.0, 90.0, 5.0, 0.70, 0.0, 80.0,
                                5.0, 2.0, 95.0, 4.5, 50.0, 0.5, 2000.0, 700.0, 1.0, 95
                        )
                ),
                new CachedForecast(
                        "key_1", 52.45, 13.20, t0.plus(Duration.ofMinutes(20)), fixedNow, fixedNow.plusSeconds(3600), WeatherModel.BEST_MATCH,
                        new WeatherCondition(
                                20.0, 180.0, 32.0, 30.0, 0.5, 0.30, 0.0, 10.0,
                                12.0, 10.0, 70.0, 11.0, 300.0, 3.0, 15000.0, 260.0, 4.0, 61
                        )
                ),
                new CachedForecast(
                        "key_2", 52.39, 13.06, t0.plus(Duration.ofMinutes(40)), fixedNow, fixedNow.plusSeconds(3600), WeatherModel.BEST_MATCH,
                        new WeatherCondition(
                                10.0, 180.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                15.0, 15.0, 50.0, 5.0, 400.0, 12.0, 30000.0, 50.0, 3.0, 1
                        )
                )
        );
        when(weatherCacheService.getForecastsForSegments(anyList())).thenReturn(extremeForecasts);
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.hiddenParameterInsights()).isNotEmpty();
        // Check wet road status is WARNING since maxWetRoad == 70% >= 60%
        HiddenParameterInsightDto wetInsight = response.hiddenParameterInsights().stream()
                .filter(i -> i.parameterKey().equals("WET_ROAD_INDEX"))
                .findFirst().orElseThrow();
        assertThat(wetInsight.status()).isEqualTo("WARNING");

        // Check gust shear is CRITICAL since maxGust == 50 km/h >= 45 km/h
        HiddenParameterInsightDto gustInsight = response.hiddenParameterInsights().stream()
                .filter(i -> i.parameterKey().equals("WIND_GUST_SHEAR"))
                .findFirst().orElseThrow();
        assertThat(gustInsight.status()).isEqualTo("CRITICAL");

        // Check soil is CRITICAL since minSoilTemp == 0.5 <= 1.0
        HiddenParameterInsightDto soilInsight = response.hiddenParameterInsights().stream()
                .filter(i -> i.parameterKey().equals("SOIL_AND_THERMAL"))
                .findFirst().orElseThrow();
        assertThat(soilInsight.status()).isEqualTo("CRITICAL");

        // Check CAPE is CRITICAL since maxCape == 700 >= 600
        HiddenParameterInsightDto capeInsight = response.hiddenParameterInsights().stream()
                .filter(i -> i.parameterKey().equals("CAPE_AND_VISIBILITY"))
                .findFirst().orElseThrow();
        assertThat(capeInsight.status()).isEqualTo("CRITICAL");
    }

    @Test
    void testsDifferentRiskLevelsAndPhysicsSynthesisBranches() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-risk-moderate");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(inv -> {
            List<RouteSegmenter.PlannedRouteSegment> planned = inv.getArgument(0);
            return planned.stream().map(s -> new CachedForecast(
                    "key_" + s.segmentIndex(),
                    s.waypointLatitude(),
                    s.waypointLongitude(),
                    s.expectedArrivalTime(),
                    fixedNow,
                    fixedNow.plusSeconds(3600),
                    WeatherModel.BEST_MATCH,
                    new WeatherCondition(
                            18.0, 180.0, 22.0, 60.0, 1.2, 0.4, 0.0, 10.0,
                            16.0, 15.0, 80.0, 12.0, 200.0, 14.0, 15000.0, 100.0, 2.0, 61
                    )
            )).toList();
        });
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.riskLevel()).isIn("MODERATE", "HIGH", "CRITICAL");
        assertThat(response.overallSummary()).contains("Punkte");
    }

    @Test
    void testsHighOverallScoreBranchInPhysicsSynthesis() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-score-high");
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(inv -> {
            List<RouteSegmenter.PlannedRouteSegment> planned = inv.getArgument(0);
            return planned.stream().map(s -> new CachedForecast(
                    "key_" + s.segmentIndex(),
                    s.waypointLatitude(),
                    s.waypointLongitude(),
                    s.expectedArrivalTime(),
                    fixedNow,
                    fixedNow.plusSeconds(3600),
                    WeatherModel.BEST_MATCH,
                    new WeatherCondition(
                            10.0, 180.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                            21.0, 21.0, 50.0, 10.0, 450.0, 19.0, 30000.0, 20.0, 3.0, 1
                    )
            )).toList();
        });
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.headline()).contains("Überwiegend gute Wetterbedingungen");
        assertThat(response.recommendations()).anyMatch(r -> r.contains("Gleichmäßiges Tempo"));
    }

    @Test
    void reusesExistingEvaluationWhenPresentWithin15Minutes() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-existing");
        Instant depTime = request.routeSnapshot().departureTime();

        WeatherEvaluation existing = new WeatherEvaluation(
                "wev-exist", "calc-existing", "user-1", EvaluationStatus.COMPLETED,
                depTime, 85, "1.0", false, depTime, depTime.plusSeconds(3600), fixedNow,
                List.of(new WeatherSegment(
                        0, depTime, 85, "#52B788", 0,
                        new WeatherCondition(10.0, 180.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 20.0, 50.0, 8.0, 400.0, 18.0, 30000.0, 20.0, 3.0, 1),
                        List.of(), null, null
                ))
        );
        weatherMcpTools.registerEphemeralEvaluation("calc-existing", existing);
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response).isNotNull();
        verify(weatherCacheService, never()).getForecastsForSegments(any());
    }

    @Test
    void reusesPersistedEvaluationWhenCacheServiceThrowsException() {
        RouteWeatherAiAnalysisRequest request = createSampleRequest("BICYCLE", "calc-throw");
        Instant depTime = request.routeSnapshot().departureTime().plus(Duration.ofHours(2)); // > 15m away so first check misses

        WeatherEvaluation existing = new WeatherEvaluation(
                "wev-throw", "calc-throw", "user-1", EvaluationStatus.COMPLETED,
                depTime, 85, "1.0", false, depTime, depTime.plusSeconds(3600), fixedNow,
                List.of(new WeatherSegment(
                        0, depTime, 85, "#52B788", 0,
                        new WeatherCondition(10.0, 180.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 20.0, 50.0, 8.0, 400.0, 18.0, 30000.0, 20.0, 3.0, 1),
                        List.of(), null, null
                ))
        );
        weatherMcpTools.registerEphemeralEvaluation("calc-throw", existing);
        when(weatherCacheService.getForecastsForSegments(any())).thenThrow(new RuntimeException("OpenMeteo unavailable"));
        when(geminiClient.isConfigured()).thenReturn(false);

        RouteWeatherAiAnalysisResponse response = service.analyzeRouteWeather(request);

        assertThat(response).isNotNull();
        assertThat(response.overallScore()).isEqualTo(85);
    }
}
