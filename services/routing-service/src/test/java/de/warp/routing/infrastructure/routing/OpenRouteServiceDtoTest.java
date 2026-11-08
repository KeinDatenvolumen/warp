// [KI-Log Nr. KI-154] (2dcf6bf) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-170] (3b5d8a2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.infrastructure.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class OpenRouteServiceDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsRequestWithLongitudeBeforeLatitude() {
        OpenRouteServiceRequest request =
                OpenRouteServiceRequest.between(
                        49.3536,
                        9.1511,
                        49.4924,
                        9.7736
                );

        assertThat(request.coordinates()).containsExactly(
                List.of(9.1511, 49.3536),
                List.of(9.7736, 49.4924)
        );

        assertThat(request.instructions()).isTrue();
    }

    @Test
    void readsRequiredRouteDataFromResponse() {
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

        OpenRouteServiceResponse response =
                objectMapper.readValue(
                        responseBody,
                        OpenRouteServiceResponse.class
                );

        assertThat(response.type())
                .isEqualTo("FeatureCollection");

        assertThat(response.features()).hasSize(1);

        OpenRouteServiceResponse.Feature feature =
                response.features().getFirst();

        assertThat(feature.geometry().type())
                .isEqualTo("LineString");

        assertThat(feature.geometry().coordinates())
                .containsExactly(
                        List.of(9.1511, 49.3536),
                        List.of(9.7736, 49.4924)
                );

        assertThat(feature.properties().summary().distance())
                .isEqualTo(57600);

        assertThat(feature.properties().summary().duration())
                .isEqualTo(11520);

        assertThat(feature.properties().segments()).hasSize(1);

        assertThat(
                feature.properties()
                        .segments()
                        .getFirst()
                        .distance()
        ).isEqualTo(57600);
    }

    @Test
    void ignoresUnknownProviderFields() {
        String responseBody = """
                {
                  "type": "FeatureCollection",
                  "providerSpecificField": "ignored",
                  "features": []
                }
                """;

        OpenRouteServiceResponse response =
                objectMapper.readValue(
                        responseBody,
                        OpenRouteServiceResponse.class
                );

        assertThat(response.type())
                .isEqualTo("FeatureCollection");

        assertThat(response.features()).isEmpty();
    }
}