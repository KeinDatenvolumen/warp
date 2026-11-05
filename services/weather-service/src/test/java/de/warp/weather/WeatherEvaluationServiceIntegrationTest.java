// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import de.warp.weather.api.EvaluateRouteWeatherRequest;
import de.warp.weather.api.InternalWeatherForecastController;
import de.warp.weather.api.WeatherEvaluationResponse;
import de.warp.weather.domain.model.EvaluationStatus;
import de.warp.weather.infrastructure.persistence.WeatherCacheRepository;
import de.warp.weather.infrastructure.persistence.WeatherEvaluationRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class WeatherEvaluationServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    static WireMockServer wireMockServer =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("warp.weather.open-meteo.base-url", wireMockServer::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Autowired
    private InternalWeatherForecastController controller;

    @Autowired
    private WeatherEvaluationRepository evaluationRepository;

    @Autowired
    private WeatherCacheRepository cacheRepository;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        evaluationRepository.deleteAll();
        cacheRepository.deleteAll();
    }

    @Test
    void evaluatesRouteSnapshotCreatesSegmentsCachesForecastsAndPersistsEvaluation() throws Exception {
        String sampleJson;
        try (InputStream is = getClass().getResourceAsStream("/openmeteo/forecast-multiple-locations.json")) {
            sampleJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        wireMockServer.stubFor(
                get(urlPathEqualTo("/v1/forecast"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(sampleJson))
        );

        Instant departureTime = Instant.ofEpochSecond(1791892800L);
        EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                "usr_test_1",
                new EvaluateRouteWeatherRequest.RouteSnapshotRequest(
                        "calc_route_1",
                        departureTime,
                        3600,
                        8000,
                        List.of(
                                List.of(9.1500, 49.3500),
                                List.of(9.1500, 49.4220) // ~8 km -> 2 segments for BICYCLE (5 km interval)
                        ),
                        new EvaluateRouteWeatherRequest.ActivityProfileSnapshotRequest(
                                "BICYCLE",
                                18.0,
                                5
                        )
                )
        );

        WeatherEvaluationResponse response = controller.evaluateRouteWeather(request);

        assertThat(response.evaluationId()).startsWith("wev_");
        assertThat(response.routeCalculationId()).isEqualTo("calc_route_1");
        assertThat(response.status()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(response.overallScore()).isBetween(0, 100);
        assertThat(response.segments()).hasSize(2);
        assertThat(response.segments().get(0).color()).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(response.segments().get(0).weather().temperatureCelsius()).isEqualTo(12.4);
        assertThat(response.segments().get(0).geometry().coordinates()).hasSizeGreaterThanOrEqualTo(2);

        // Verify cache entries were persisted
        assertThat(cacheRepository.count()).isEqualTo(2);
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/v1/forecast")));

        // Evaluating a second route at the same waypoints & hour hits the cache without calling Open-Meteo again
        EvaluateRouteWeatherRequest secondRequest = new EvaluateRouteWeatherRequest(
                "usr_test_1",
                new EvaluateRouteWeatherRequest.RouteSnapshotRequest(
                        "calc_route_2",
                        departureTime,
                        3600,
                        8000,
                        List.of(
                                List.of(9.1500, 49.3500),
                                List.of(9.1500, 49.4220)
                        ),
                        new EvaluateRouteWeatherRequest.ActivityProfileSnapshotRequest(
                                "BICYCLE",
                                18.0,
                                5
                        )
                )
        );

        WeatherEvaluationResponse secondResponse = controller.evaluateRouteWeather(secondRequest);
        assertThat(secondResponse.overallScore()).isEqualTo(response.overallScore());
        // Still only 1 HTTP call to Open-Meteo thanks to the 60-minute cache!
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/v1/forecast")));

        // Verify retrieval by evaluationId and by routeCalculationId
        WeatherEvaluationResponse byId = controller.getEvaluation(response.evaluationId());
        assertThat(byId.evaluationId()).isEqualTo(response.evaluationId());

        WeatherEvaluationResponse byRoute = controller.getEvaluationByRoute("calc_route_1");
        assertThat(byRoute.evaluationId()).isEqualTo(response.evaluationId());
    }
}
