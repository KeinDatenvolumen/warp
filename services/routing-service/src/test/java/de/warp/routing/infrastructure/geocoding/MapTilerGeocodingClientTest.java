// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
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