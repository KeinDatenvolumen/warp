package de.warp.routing.infrastructure.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenRouteServiceClientTest {

    private MockRestServiceServer mockServer;
    private OpenRouteServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .baseUrl(
                                "https://api.openrouteservice.org"
                        );

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        client = new OpenRouteServiceClient(
                restClientBuilder.build(),
                "test-api-key"
        );
    }

    @Test
    void calculatesRouteUsingConfiguredProfileAndCoordinates() {
        String responseBody = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [
                          [9.1511, 49.3536],
                          [9.7736, 49.4924]
                        ]
                      },
                      "properties": {
                        "summary": {
                          "distance": 57600,
                          "duration": 11520
                        },
                        "segments": [
                          {
                            "distance": 57600,
                            "duration": 11520
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(
                        requestTo(
                                "https://api.openrouteservice.org"
                                        + "/v2/directions/"
                                        + "cycling-regular/geojson"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        header(
                                "Authorization",
                                "test-api-key"
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
                                  "coordinates": [
                                    [9.1511, 49.3536],
                                    [9.7736, 49.4924]
                                  ],
                                  "instructions": false
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        OpenRouteServiceResponse.Feature route =
                client.calculateRoute(
                        "cycling-regular",
                        49.3536,
                        9.1511,
                        49.4924,
                        9.7736
                );

        assertThat(route.geometry().type())
                .isEqualTo("LineString");

        assertThat(route.properties().summary().distance())
                .isEqualTo(57600);

        assertThat(route.properties().summary().duration())
                .isEqualTo(11520);

        assertThat(route.properties().segments())
                .hasSize(1);

        mockServer.verify();
    }
}