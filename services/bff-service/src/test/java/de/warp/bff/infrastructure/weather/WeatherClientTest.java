// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.infrastructure.weather;

import de.warp.bff.infrastructure.weather.WeatherApiDtos.ActivityProfileSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.EvaluateRouteWeatherRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteSnapshotRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.WeatherEvaluationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("WeatherClient Integration & Error Handling Tests")
class WeatherClientTest {

    private static final String BASE_URL = "http://weather-service:8083";

    private MockRestServiceServer mockServer;
    private WeatherClient weatherClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        weatherClient = new WeatherClient(restClientBuilder.build());
    }

    private RouteSnapshotRequest createSampleRouteSnapshot() {
        return new RouteSnapshotRequest(
                "calc_123",
                Instant.parse("2026-10-22T08:00:00Z"),
                3600,
                25000,
                List.of(List.of(9.15, 49.35), List.of(9.20, 49.40)),
                new ActivityProfileSnapshotRequest("ROAD_CYCLING", 25.0, 5)
        );
    }

    @Nested
    @DisplayName("evaluateRouteWeather Tests")
    class EvaluateRouteWeatherTests {

        @Test
        @DisplayName("Successfully evaluates route weather and deserializes response")
        void evaluatesRouteWeatherSuccessfully() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            """
                            {
                              "evaluationId": "eval_100",
                              "routeCalculationId": "calc_123",
                              "requestedByUserId": "user_456",
                              "status": "COMPLETED",
                              "departureTime": "2026-10-22T08:00:00Z",
                              "overallScore": 88,
                              "scoreVersion": "v1.0",
                              "thunderstormCapApplied": false,
                              "weatherDataGeneratedAt": "2026-10-22T07:50:00Z",
                              "weatherDataExpiresAt": "2026-10-22T09:00:00Z",
                              "createdAt": "2026-10-22T07:50:00Z",
                              "segments": []
                            }
                            """,
                            MediaType.APPLICATION_JSON
                    ));

            EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                    "user_456",
                    createSampleRouteSnapshot()
            );

            WeatherEvaluationResponse response = weatherClient.evaluateRouteWeather(request);

            assertThat(response).isNotNull();
            assertThat(response.evaluationId()).isEqualTo("eval_100");
            assertThat(response.overallScore()).isEqualTo(88);
            assertThat(response.status()).isEqualTo("COMPLETED");
            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpClientErrorException on 400 Bad Request")
        void throwsOnBadRequest400() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST));

            EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                    "user_456",
                    createSampleRouteSnapshot()
            );

            assertThatThrownBy(() -> weatherClient.evaluateRouteWeather(request))
                    .isInstanceOf(HttpClientErrorException.class)
                    .extracting(e -> ((HttpClientErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpServerErrorException on 500 Internal Server Error")
        void throwsOnServerError500() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                    "user_456",
                    createSampleRouteSnapshot()
            );

            assertThatThrownBy(() -> weatherClient.evaluateRouteWeather(request))
                    .isInstanceOf(HttpServerErrorException.class)
                    .extracting(e -> ((HttpServerErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws ResourceAccessException on downstream timeout")
        void throwsOnTimeout() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new SocketTimeoutException("Read timed out")));

            EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                    "user_456",
                    createSampleRouteSnapshot()
            );

            assertThatThrownBy(() -> weatherClient.evaluateRouteWeather(request))
                    .isInstanceOf(ResourceAccessException.class);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws RestClientException on malformed JSON response")
        void throwsOnMalformedResponse() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("{\"evaluationId\":", MediaType.APPLICATION_JSON));

            EvaluateRouteWeatherRequest request = new EvaluateRouteWeatherRequest(
                    "user_456",
                    createSampleRouteSnapshot()
            );

            assertThatThrownBy(() -> weatherClient.evaluateRouteWeather(request))
                    .isInstanceOf(RestClientException.class);

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("getEvaluationById Tests")
    class GetEvaluationByIdTests {

        @Test
        @DisplayName("Successfully retrieves evaluation by ID")
        void retrievesEvaluationByIdSuccessfully() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations/eval_999"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(
                            """
                            {
                              "evaluationId": "eval_999",
                              "routeCalculationId": "calc_123",
                              "requestedByUserId": "usr_abc",
                              "status": "COMPLETED",
                              "departureTime": "2026-10-22T08:00:00Z",
                              "overallScore": 92,
                              "scoreVersion": "v1.0",
                              "thunderstormCapApplied": false,
                              "weatherDataGeneratedAt": "2026-10-22T07:50:00Z",
                              "weatherDataExpiresAt": "2026-10-22T09:00:00Z",
                              "createdAt": "2026-10-22T07:50:00Z",
                              "segments": []
                            }
                            """,
                            MediaType.APPLICATION_JSON
                    ));

            WeatherEvaluationResponse response = weatherClient.getEvaluationById("eval_999");

            assertThat(response).isNotNull();
            assertThat(response.evaluationId()).isEqualTo("eval_999");
            assertThat(response.overallScore()).isEqualTo(92);
            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpClientErrorException on 404 Not Found")
        void throwsOnNotFound404() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations/eval_missing"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> weatherClient.getEvaluationById("eval_missing"))
                    .isInstanceOf(HttpClientErrorException.class)
                    .extracting(e -> ((HttpClientErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpServerErrorException on 503 Service Unavailable")
        void throwsOnServiceUnavailable503() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations/eval_503"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            assertThatThrownBy(() -> weatherClient.getEvaluationById("eval_503"))
                    .isInstanceOf(HttpServerErrorException.class)
                    .extracting(e -> ((HttpServerErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("getEvaluationByRoute Tests")
    class GetEvaluationByRouteTests {

        @Test
        @DisplayName("Successfully retrieves evaluation by route calculation ID")
        void retrievesEvaluationByRouteSuccessfully() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations/by-route/calc_route_123"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(
                            """
                            {
                              "evaluationId": "eval_from_route",
                              "routeCalculationId": "calc_route_123",
                              "requestedByUserId": "usr_route",
                              "status": "COMPLETED",
                              "departureTime": "2026-10-22T08:00:00Z",
                              "overallScore": 77,
                              "scoreVersion": "v1.0",
                              "thunderstormCapApplied": true,
                              "weatherDataGeneratedAt": "2026-10-22T07:50:00Z",
                              "weatherDataExpiresAt": "2026-10-22T09:00:00Z",
                              "createdAt": "2026-10-22T07:50:00Z",
                              "segments": []
                            }
                            """,
                            MediaType.APPLICATION_JSON
                    ));

            WeatherEvaluationResponse response = weatherClient.getEvaluationByRoute("calc_route_123");

            assertThat(response).isNotNull();
            assertThat(response.evaluationId()).isEqualTo("eval_from_route");
            assertThat(response.thunderstormCapApplied()).isTrue();
            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpClientErrorException on 404 Not Found")
        void throwsOnNotFound404() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations/by-route/calc_unknown"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> weatherClient.getEvaluationByRoute("calc_unknown"))
                    .isInstanceOf(HttpClientErrorException.class)
                    .extracting(e -> ((HttpClientErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpServerErrorException on 500 Internal Server Error")
        void throwsOnServerError500() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/evaluations/by-route/calc_err"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> weatherClient.getEvaluationByRoute("calc_err"))
                    .isInstanceOf(HttpServerErrorException.class)
                    .extracting(e -> ((HttpServerErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("optimizeDeparture Tests")
    class OptimizeDepartureTests {

        @Test
        @DisplayName("Successfully optimizes departure and deserializes response")
        void optimizesDepartureSuccessfully() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/optimization"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            """
                            {
                              "recommendedSlotIndex": 1,
                              "optimalWindowSummary": {
                                "bestDeparture": "08:30",
                                "bestDepartureIso": "2026-10-22T08:30:00Z",
                                "bestScore": 95,
                                "stableWindow": "08:30 - 09:30",
                                "advantageVsWorst": "+25 Punkte",
                                "worstDeparture": "07:00",
                                "worstScore": 70
                              },
                              "departureSlots": [
                                {
                                  "departureIso": "2026-10-22T08:30:00Z",
                                  "departureTime": "08:30",
                                  "arrivalIso": "2026-10-22T09:30:00Z",
                                  "arrivalTime": "09:30",
                                  "routeScore": 95,
                                  "robustScore": 93.5,
                                  "hasThunderCap": false,
                                  "reachesNightfall": false,
                                  "segmentScores": [95, 95],
                                  "segmentReasons": [],
                                  "summaryDeltaText": "Bester Slot"
                                }
                              ]
                            }
                            """,
                            MediaType.APPLICATION_JSON
                    ));

            RouteWeatherOptimizationRequest request = new RouteWeatherOptimizationRequest(
                    "usr_123",
                    createSampleRouteSnapshot(),
                    Instant.parse("2026-10-22T07:00:00Z"),
                    Instant.parse("2026-10-22T10:00:00Z")
            );

            RouteWeatherOptimizationResponse response = weatherClient.optimizeDeparture(request);

            assertThat(response).isNotNull();
            assertThat(response.recommendedSlotIndex()).isEqualTo(1);
            assertThat(response.optimalWindowSummary().bestScore()).isEqualTo(95);
            assertThat(response.departureSlots()).hasSize(1);
            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpClientErrorException on 400 Bad Request")
        void throwsOnBadRequest400() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/optimization"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST));

            RouteWeatherOptimizationRequest request = new RouteWeatherOptimizationRequest(
                    "usr_123",
                    createSampleRouteSnapshot(),
                    Instant.parse("2026-10-22T07:00:00Z"),
                    Instant.parse("2026-10-22T10:00:00Z")
            );

            assertThatThrownBy(() -> weatherClient.optimizeDeparture(request))
                    .isInstanceOf(HttpClientErrorException.class);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpServerErrorException on 500 Internal Server Error")
        void throwsOnServerError500() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/optimization"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            RouteWeatherOptimizationRequest request = new RouteWeatherOptimizationRequest(
                    "usr_123",
                    createSampleRouteSnapshot(),
                    Instant.parse("2026-10-22T07:00:00Z"),
                    Instant.parse("2026-10-22T10:00:00Z")
            );

            assertThatThrownBy(() -> weatherClient.optimizeDeparture(request))
                    .isInstanceOf(HttpServerErrorException.class);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws ResourceAccessException on network timeout")
        void throwsOnTimeout() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/optimization"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new SocketTimeoutException("Connection timed out")));

            RouteWeatherOptimizationRequest request = new RouteWeatherOptimizationRequest(
                    "usr_123",
                    createSampleRouteSnapshot(),
                    Instant.parse("2026-10-22T07:00:00Z"),
                    Instant.parse("2026-10-22T10:00:00Z")
            );

            assertThatThrownBy(() -> weatherClient.optimizeDeparture(request))
                    .isInstanceOf(ResourceAccessException.class);

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("analyzeRouteWeatherAi Tests")
    class AnalyzeRouteWeatherAiTests {

        @Test
        @DisplayName("Successfully triggers AI analysis and deserializes response")
        void analyzesRouteWeatherAiSuccessfully() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/ai-analysis"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            """
                            {
                              "routeCalculationId": "calc_ai",
                              "activityProfileCode": "ROAD_CYCLING",
                              "activityProfileDisplayName": "Rennrad",
                              "departureTime": "2026-10-22T08:00:00Z",
                              "overallScore": 88,
                              "thunderstormCapApplied": false,
                              "riskLevel": "LOW",
                              "headline": "Ideales Rennradwetter",
                              "overallSummary": "Sehr gute Bedingungen auf der gesamten Strecke.",
                              "activityProfileAdvice": "Windjacke für den Start empfohlen.",
                              "hiddenParameterInsights": [
                                {
                                  "parameterKey": "wetRoadIndex",
                                  "parameterLabel": "Nasse Fahrbahn",
                                  "category": "SURFACE",
                                  "observedValue": "0.1",
                                  "status": "OPTIMAL",
                                  "interpretation": "Trockener Asphalt"
                                }
                              ],
                              "criticalSections": [],
                              "recommendations": ["Sonnenbrille mitnehmen"],
                              "mcpToolsUsed": ["fetch_road_surface_conditions"],
                              "modelName": "gemini-2.5-pro",
                              "generatedAt": "2026-10-22T07:55:00Z"
                            }
                            """,
                            MediaType.APPLICATION_JSON
                    ));

            RouteWeatherAiAnalysisRequest request = new RouteWeatherAiAnalysisRequest(
                    "usr_ai",
                    "Mosbach",
                    "Bad Mergentheim",
                    createSampleRouteSnapshot()
            );

            RouteWeatherAiAnalysisResponse response = weatherClient.analyzeRouteWeatherAi(request);

            assertThat(response).isNotNull();
            assertThat(response.routeCalculationId()).isEqualTo("calc_ai");
            assertThat(response.riskLevel()).isEqualTo("LOW");
            assertThat(response.modelName()).isEqualTo("gemini-2.5-pro");
            assertThat(response.hiddenParameterInsights()).hasSize(1);
            assertThat(response.recommendations()).containsExactly("Sonnenbrille mitnehmen");
            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpClientErrorException on 429 Too Many Requests")
        void throwsOnRateLimit429() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/ai-analysis"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

            RouteWeatherAiAnalysisRequest request = new RouteWeatherAiAnalysisRequest(
                    "usr_ai",
                    "Mosbach",
                    "Bad Mergentheim",
                    createSampleRouteSnapshot()
            );

            assertThatThrownBy(() -> weatherClient.analyzeRouteWeatherAi(request))
                    .isInstanceOf(HttpClientErrorException.class)
                    .extracting(e -> ((HttpClientErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws HttpServerErrorException on 503 Service Unavailable")
        void throwsOnServiceUnavailable503() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/ai-analysis"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            RouteWeatherAiAnalysisRequest request = new RouteWeatherAiAnalysisRequest(
                    "usr_ai",
                    "Mosbach",
                    "Bad Mergentheim",
                    createSampleRouteSnapshot()
            );

            assertThatThrownBy(() -> weatherClient.analyzeRouteWeatherAi(request))
                    .isInstanceOf(HttpServerErrorException.class)
                    .extracting(e -> ((HttpServerErrorException) e).getStatusCode())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws ResourceAccessException on network timeout")
        void throwsOnTimeout() {
            mockServer.expect(requestTo(BASE_URL + "/internal/api/v1/weather/ai-analysis"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new SocketTimeoutException("Connection timed out")));

            RouteWeatherAiAnalysisRequest request = new RouteWeatherAiAnalysisRequest(
                    "usr_ai",
                    "Mosbach",
                    "Bad Mergentheim",
                    createSampleRouteSnapshot()
            );

            assertThatThrownBy(() -> weatherClient.analyzeRouteWeatherAi(request))
                    .isInstanceOf(ResourceAccessException.class);

            mockServer.verify();
        }
    }
}
