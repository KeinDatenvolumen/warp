// [KI-Log Nr. KI-123] (00e2e64) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-137] (15d143f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import de.warp.bff.domain.model.Place;

class RoutingGeocodingClientTest {

    private MockRestServiceServer server;
    private RoutingGeocodingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://routing-service:8082");

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        client = new RoutingGeocodingClient(builder.build());
    }

    @Test
    void forwardsSearchToRoutingService() {
        server.expect(
                        requestTo(
                                "http://routing-service:8082"
                                        + "/internal/v1/places"
                                        + "?query=Stuttgart&limit=5"
                        )
                )
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                        {
                          "items": [
                            {
                              "placeId": "stuttgart",
                              "displayName": "Stuttgart, Deutschland",
                              "latitude": 48.7758,
                              "longitude": 9.1829
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        List < Place > places =
                client.searchPlaces("Stuttgart", 5);

        assertEquals(1, places.size());
        assertEquals("stuttgart", places.getFirst().placeId());
        assertEquals(
                "Stuttgart, Deutschland",
                places.getFirst().displayName()
        );
        assertEquals(48.7758, places.getFirst().latitude());
        assertEquals(9.1829, places.getFirst().longitude());

        server.verify();
    }

    @Test
    void returnsEmptyListWhenItemsAreMissing() {
        server.expect(
                        requestTo(
                                "http://routing-service:8082"
                                        + "/internal/v1/places"
                                        + "?query=Stuttgart&limit=5"
                        )
                )
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{}",
                        MediaType.APPLICATION_JSON
                ));

        List < Place > places =
                client.searchPlaces("Stuttgart", 5);

        assertEquals(List.of(), places);

        server.verify();
    }
}