// [KI-Log Nr. KI-162] (6e98a51) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-178] (503803e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match
        .MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match
        .MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match
        .MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match
        .MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response
        .MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response
        .MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response
        .MockRestResponseCreators.withSuccess;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import de.warp.bff.application.exception
        .InvalidRouteRequestException;
import de.warp.bff.application.exception
        .RouteCalculationNotFoundException;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationAcceptedResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalStartRouteCalculationRequest;
import tools.jackson.databind.json.JsonMapper;

class RoutingClientTest {

    private MockRestServiceServer mockServer;
    private RoutingClient routingClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .baseUrl("http://routing-service:8082");

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        routingClient = new RoutingClient(
                restClientBuilder.build(),
                JsonMapper.builder().build()
        );
    }

    @Test
    void startsRouteCalculationWithProfileSnapshot() {
        mockServer.expect(
                        requestTo(
                                "http://routing-service:8082"
                                        + "/internal/v1/"
                                        + "route-calculations"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        header(
                                "Idempotency-Key",
                                "idempotency-key-123"
                        )
                )
                .andExpect(
                        content().contentType(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "requestedByUserId": "usr_123",
                                  "start": {
                                    "label": "Mosbach",
                                    "latitude": 49.3536,
                                    "longitude": 9.1511
                                  },
                                  "destination": {
                                    "label": "Bad Mergentheim",
                                    "latitude": 49.4924,
                                    "longitude": 9.7736
                                  },
                                  "departureTime":
                                    "2026-10-22T06:00:00Z",
                                  "activityProfile": {
                                    "code": "BICYCLE",
                                    "routingProfile":
                                      "cycling-regular"
                                  }
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "calc_123",
                                  "status": "PENDING",
                                  "createdAt":
                                    "2026-10-21T18:00:00Z"
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        InternalRouteCalculationAcceptedResponse response =
                routingClient.startRouteCalculation(
                        "idempotency-key-123",
                        createRequest()
                );

        assertThat(response.id())
                .isEqualTo("calc_123");

        assertThat(response.status())
                .isEqualTo("PENDING");

        assertThat(response.createdAt())
                .isEqualTo(
                        Instant.parse(
                                "2026-10-21T18:00:00Z"
                        )
                );

        mockServer.verify();
    }

    @Test
    void readsCompletedRouteCalculation() {
        mockServer.expect(
                        requestTo(
                                "http://routing-service:8082"
                                        + "/internal/v1/"
                                        + "route-calculations/"
                                        + "calc_123"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "calc_123",
                                  "status": "COMPLETED",
                                  "start": {
                                    "label": "Mosbach",
                                    "latitude": 49.3536,
                                    "longitude": 9.1511
                                  },
                                  "destination": {
                                    "label": "Bad Mergentheim",
                                    "latitude": 49.4924,
                                    "longitude": 9.7736
                                  },
                                  "activityProfile": {
                                    "code": "BICYCLE",
                                    "routingProfile":
                                      "cycling-regular"
                                  },
                                  "departureTime":
                                    "2026-10-22T06:00:00Z",
                                  "arrivalTime":
                                    "2026-10-22T07:12:00Z",
                                  "distanceMeters": 57600,
                                  "durationSeconds": 4320,
                                  "geometry": {
                                    "type": "LineString",
                                    "coordinates": [
                                      [9.1511, 49.3536],
                                      [9.7736, 49.4924]
                                    ]
                                  },
                                  "segments": [
                                    {
                                      "distance": 57600,
                                      "duration": 4320
                                    }
                                  ],
                                  "createdAt":
                                    "2026-10-21T18:00:00Z",
                                  "updatedAt":
                                    "2026-10-21T18:00:03Z",
                                  "error": null
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        InternalRouteCalculationResponse response =
                routingClient.getRouteCalculation(
                        "calc_123"
                );

        assertThat(response.id())
                .isEqualTo("calc_123");

        assertThat(response.status())
                .isEqualTo("COMPLETED");

        assertThat(response.activityProfile().code())
                .isEqualTo("BICYCLE");

        assertThat(
                response.activityProfile().routingProfile()
        ).isEqualTo("cycling-regular");

        assertThat(response.distanceMeters())
                .isEqualTo(57600);

        assertThat(response.durationSeconds())
                .isEqualTo(4320);

        assertThat(response.geometry())
                .isNotNull();

        assertThat(
                response.geometry()
                        .get("type").asString()
        ).isEqualTo("LineString");

        assertThat(response.segments())
                .isNotNull();

        assertThat(response.segments().isArray())
                .isTrue();

        mockServer.verify();
    }

    @Test
    void passesUserIdHeaderWhenFetchingRouteCalculation() {
        mockServer.expect(
                        requestTo(
                                "http://routing-service:8082"
                                        + "/internal/v1/"
                                        + "route-calculations/"
                                        + "calc_123"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", "usr_123"))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "calc_123",
                                  "status": "PROCESSING",
                                  "start": {
                                    "label": "Mosbach",
                                    "latitude": 49.3536,
                                    "longitude": 9.1511
                                  },
                                  "destination": {
                                    "label": "Bad Mergentheim",
                                    "latitude": 49.4924,
                                    "longitude": 9.7736
                                  },
                                  "activityProfile": {
                                    "code": "BICYCLE",
                                    "routingProfile": "cycling-regular"
                                  },
                                  "departureTime": "2026-10-22T06:00:00Z",
                                  "arrivalTime": null,
                                  "distanceMeters": null,
                                  "durationSeconds": null,
                                  "geometry": null,
                                  "segments": null,
                                  "createdAt": "2026-10-21T18:00:00Z",
                                  "updatedAt": "2026-10-21T18:00:03Z",
                                  "error": null
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        InternalRouteCalculationResponse response =
                routingClient.getRouteCalculation(
                        "usr_123",
                        "calc_123"
                );

        assertThat(response.id()).isEqualTo("calc_123");
        assertThat(response.status()).isEqualTo("PROCESSING");
        mockServer.verify();
    }

    @Test
    void translatesInvalidRouteRequest() {
        mockServer.expect(
                        requestTo(
                                "http://routing-service:8082"
                                        + "/internal/v1/"
                                        + "route-calculations"
                        )
                )
                .andRespond(
                        withBadRequest()
                                .body(
                                        """
                                        {
                                          "type":
                                            "https://warp.local/problems/invalid-route-request",
                                          "title": "Bad Request",
                                          "status": 400,
                                          "code":
                                            "INVALID_ROUTE_REQUEST",
                                          "detail":
                                            "Die Routenanfrage ist ungültig.",
                                          "instance":
                                            "/internal/v1/route-calculations",
                                          "correlationId":
                                            "correlation-123",
                                          "fieldErrors": []
                                        }
                                        """
                                )
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                );

        assertThatThrownBy(
                () -> routingClient.startRouteCalculation(
                        "idempotency-key-123",
                        createRequest()
                )
        ).isInstanceOf(
                InvalidRouteRequestException.class
        );

        mockServer.verify();
    }

    @Test
    void translatesMissingRouteCalculation() {
        mockServer.expect(
                        requestTo(
                                "http://routing-service:8082"
                                        + "/internal/v1/"
                                        + "route-calculations/"
                                        + "calc_missing"
                        )
                )
                .andRespond(
                        withResourceNotFound()
                                .body(
                                        """
                                        {
                                          "type":
                                            "https://warp.local/problems/route-calculation-not-found",
                                          "title": "Not Found",
                                          "status": 404,
                                          "code":
                                            "ROUTE_CALCULATION_NOT_FOUND",
                                          "detail":
                                            "Die Routenberechnung wurde nicht gefunden.",
                                          "instance":
                                            "/internal/v1/route-calculations/calc_missing",
                                          "correlationId":
                                            "correlation-123",
                                          "fieldErrors": []
                                        }
                                        """
                                )
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                );

        assertThatThrownBy(
                () -> routingClient.getRouteCalculation(
                        "calc_missing"
                )
        ).isInstanceOf(
                RouteCalculationNotFoundException.class
        );

        mockServer.verify();
    }

    private InternalStartRouteCalculationRequest
    createRequest() {
        return new InternalStartRouteCalculationRequest(
                "usr_123",
                new InternalStartRouteCalculationRequest
                        .RoutePoint(
                        "Mosbach",
                        49.3536,
                        9.1511
                ),
                new InternalStartRouteCalculationRequest
                        .RoutePoint(
                        "Bad Mergentheim",
                        49.4924,
                        9.7736
                ),
                Instant.parse(
                        "2026-10-22T06:00:00Z"
                ),
                new InternalStartRouteCalculationRequest
                        .ActivityProfileSnapshot(
                        "BICYCLE",
                        "cycling-regular"
                )
        );
    }
}