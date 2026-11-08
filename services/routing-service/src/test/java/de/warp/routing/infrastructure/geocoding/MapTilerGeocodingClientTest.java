// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.infrastructure.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import de.warp.routing.domain.model.Place;

class MapTilerGeocodingClientTest {

    private MapTilerGeocodingClient client;

    @BeforeEach
    void setUp() {
        client = new MapTilerGeocodingClient(
                RestClient.create(),
                "test-key"
        );
    }

    @Test
    void mapsValidCoordinates() {
        MapTilerGeocodingResponse.Feature feature =
                new MapTilerGeocodingResponse.Feature(
                        "stuttgart",
                        "Stuttgart, Deutschland",
                        List.of(9.1829, 48.7758)
                );

        Place result = client.toPlace(feature);

        assertEquals("stuttgart", result.placeId());
        assertEquals("Stuttgart, Deutschland", result.displayName());
        assertEquals(48.7758, result.latitude());
        assertEquals(9.1829, result.longitude());
    }

    @Test
    void rejectsLatitudeOutsideValidRange() {
        MapTilerGeocodingResponse.Feature feature =
                new MapTilerGeocodingResponse.Feature(
                        "invalid",
                        "Ungültiger Ort",
                        List.of(9.1829, 91.0)
                );

        assertNull(client.toPlace(feature));
    }

    @Test
    void rejectsLongitudeOutsideValidRange() {
        MapTilerGeocodingResponse.Feature feature =
                new MapTilerGeocodingResponse.Feature(
                        "invalid",
                        "Ungültiger Ort",
                        List.of(181.0, 48.7758)
                );

        assertNull(client.toPlace(feature));
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        MapTilerGeocodingResponse.Feature feature =
                new MapTilerGeocodingResponse.Feature(
                        "invalid",
                        "Ungültiger Ort",
                        List.of(Double.NaN, 48.7758)
                );

        assertNull(client.toPlace(feature));
    }

    @Test
    void rejectsMissingCoordinates() {
        MapTilerGeocodingResponse.Feature feature =
                new MapTilerGeocodingResponse.Feature(
                        "invalid",
                        "Ungültiger Ort",
                        List.of(9.1829)
                );

        assertNull(client.toPlace(feature));
    }
}