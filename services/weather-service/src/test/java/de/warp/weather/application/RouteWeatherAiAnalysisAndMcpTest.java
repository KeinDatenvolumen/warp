package de.warp.weather.application;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.warp.weather.api.EvaluateRouteWeatherRequest.ActivityProfileSnapshotRequest;
import de.warp.weather.api.EvaluateRouteWeatherRequest.RouteSnapshotRequest;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisRequest;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.domain.model.CachedForecast;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.infrastructure.gemini.GeminiClient;
import de.warp.weather.infrastructure.gemini.GeminiConfiguration;
import de.warp.weather.infrastructure.gemini.GeminiProperties;
import de.warp.weather.infrastructure.mcp.McpServerController;
import de.warp.weather.infrastructure.mcp.McpToolRegistry;
import de.warp.weather.infrastructure.mcp.WeatherMcpTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteWeatherAiAnalysisAndMcpTest {

    private WireMockServer wireMockServer;
    private ObjectMapper objectMapper;
    private WeatherScoringService scoringService;
    private WeatherMcpTools weatherMcpTools;
    private McpToolRegistry mcpToolRegistry;
    private McpServerController mcpServerController;
    private WeatherCacheService weatherCacheService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        objectMapper = new ObjectMapper();
        scoringService = new WeatherScoringService();
        weatherMcpTools = new WeatherMcpTools(scoringService, null);
        mcpToolRegistry = new McpToolRegistry(weatherMcpTools, objectMapper);
        mcpServerController = new McpServerController(mcpToolRegistry, objectMapper);

        weatherCacheService = mock(WeatherCacheService.class);
        when(weatherCacheService.getForecastsForSegments(anyList())).thenAnswer(invocation -> {
            List<RouteSegmenter.PlannedRouteSegment> segs = invocation.getArgument(0);
            Instant now = Instant.parse("2026-05-16T08:00:00Z");
            return segs.stream()
                    .map(s -> new CachedForecast(
                            "key_" + s.segmentIndex(),
                            s.waypointLatitude(),
                            s.waypointLongitude(),
                            s.expectedArrivalTime(),
                            now,
                            now.plusSeconds(3600),
                            WeatherModel.BEST_MATCH,
                            new WeatherCondition(
                                    22.0,
                                    90.0,
                                    44.0,
                                    40.0,
                                    0.2,
                                    0.65,
                                    0.0,
                                    25.0,
                                    14.0,
                                    11.5,
                                    88.0,
                                    12.2,
                                    420.0,
                                    8.5,
                                    9500.0,
                                    480.0,
                                    4.5,
                                    61
                            )
                    ))
                    .toList();
        });
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void mcpServerSupportsInitializeToolsListAndToolsCall() {
        ResponseEntity<Map<String, Object>> initResp = mcpServerController.handleMcpJsonRpc(Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize"
        ));
        assertThat(initResp.getBody()).isNotNull();
        assertThat(initResp.getBody()).containsEntry("id", 1);

        ResponseEntity<Map<String, Object>> listResp = mcpServerController.handleMcpJsonRpc(Map.of(
                "jsonrpc", "2.0",
                "id", 2,
                "method", "tools/list"
        ));
        assertThat(listResp.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) listResp.getBody().get("result");
        assertThat(result).containsKey("tools");

        ResponseEntity<Map<String, Object>> callResp = mcpServerController.handleMcpJsonRpc(Map.of(
                "jsonrpc", "2.0",
                "id", 3,
                "method", "tools/call",
                "params", Map.of(
                        "name", "get_activity_profile_vulnerability_matrix",
                        "arguments", Map.of("activityProfileCode", "MOTORCYCLE")
                )
        ));
        assertThat(callResp.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> callResult = (Map<String, Object>) callResp.getBody().get("result");
        assertThat(callResult).containsEntry("isError", false);
    }

    @Test
    void callsGeminiApiWithMcpContextAndParsesStructuredResponse() {
        String geminiJsonOutput = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"riskLevel\\":\\"HIGH\\",\\"headline\\":\\"Vorsicht für Motorradfahrer: Restnässe & Seitenwindböen\\",\\"overallSummary\\":\\"Auf der Gesamtroute von Mosbach nach Bad Mergentheim zeigt der 2h-Fahrbahnnässe-Index 65% Nässe bei Böen bis 44 km/h.\\",\\"activityProfileAdvice\\":\\"Visier-Pinlock nutzen (Taupunkt-Spread 1,8°C) und Schräglage in Waldkurven reduzieren.\\",\\"hiddenParameterInsights\\":[{\\"parameterKey\\":\\"WET_ROAD_INDEX\\",\\"parameterLabel\\":\\"Fahrbahnnässe-Index\\",\\"category\\":\\"ROAD_SURFACE\\",\\"observedValue\\":\\"65 % Restnässe\\",\\"status\\":\\"WARNING\\",\\"interpretation\\":\\"Asphalt bleibt nach Vorregen rutschig.\\"}],\\"criticalSections\\":[{\\"segmentIndexStart\\":0,\\"segmentIndexEnd\\":1,\\"distanceRangeKm\\":\\"km 0,0 – 10,0\\",\\"timeWindow\\":\\"10:00 – 10:15 Uhr\\",\\"minSegmentScore\\":58,\\"severity\\":\\"WARNING\\",\\"primaryFactor\\":\\"WIND\\",\\"title\\":\\"Seitenwindböen & feuchter Belag\\",\\"description\\":\\"Böen bis 44 km/h treffen seitlich auf nasse Fahrbahn.\\",\\"recommendation\\":\\"Tempo drosseln.\\"}],\\"recommendations\\":[\\"Pinlock-Visier prüfen\\",\\"Kurvengeschwindigkeit anpassen\\"]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/v1beta/models/gemini-3.5-flash:generateContent"))
                .withHeader("x-goog-api-key", equalTo("test-gemini-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(geminiJsonOutput)));

        GeminiProperties geminiProps = new GeminiProperties(
                URI.create(wireMockServer.baseUrl()),
                "test-gemini-key",
                "gemini-3.5-flash",
                "gemini-3.6-flash",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                0.2
        );
        RestClient geminiRestClient = new GeminiConfiguration().geminiRestClient(geminiProps);
        GeminiClient geminiClient = new GeminiClient(geminiRestClient, geminiProps, objectMapper);

        RouteWeatherAiAnalysisService aiService = new RouteWeatherAiAnalysisService(
                new RouteSegmenter(),
                weatherCacheService,
                scoringService,
                weatherMcpTools,
                mcpToolRegistry,
                geminiClient,
                objectMapper,
                Clock.fixed(Instant.parse("2026-05-16T07:00:00Z"), ZoneOffset.UTC)
        );

        RouteWeatherAiAnalysisRequest request = new RouteWeatherAiAnalysisRequest(
                "usr_verified_1",
                "Mosbach",
                "Bad Mergentheim",
                new RouteSnapshotRequest(
                        "calc_ai_1",
                        Instant.parse("2026-05-16T08:00:00Z"),
                        2700,
                        35000,
                        List.of(
                                List.of(9.15, 49.35),
                                List.of(9.45, 49.42),
                                List.of(9.77, 49.49)
                        ),
                        new ActivityProfileSnapshotRequest("MOTORCYCLE", 80.0, 10)
                )
        );

        RouteWeatherAiAnalysisResponse response = aiService.analyzeRouteWeather(request);

        assertThat(response.riskLevel()).isEqualTo("HIGH");
        assertThat(response.activityProfileCode()).isEqualTo("MOTORCYCLE");
        assertThat(response.headline()).contains("Motorradfahrer");
        assertThat(response.modelName()).isEqualTo("gemini-3.5-flash");
        assertThat(response.mcpToolsUsed()).containsExactly(
                "get_route_weather_telemetry",
                "detect_hidden_meteorological_anomalies",
                "get_activity_profile_vulnerability_matrix"
        );
        assertThat(response.hiddenParameterInsights()).isNotEmpty();
        assertThat(response.criticalSections()).isNotEmpty();

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1beta/models/gemini-3.5-flash:generateContent"))
                .withHeader("x-goog-api-key", equalTo("test-gemini-key")));
    }
}
