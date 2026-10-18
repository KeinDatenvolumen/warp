package de.warp.routing.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.warp.routing.domain.model.Place;
import de.warp.routing.infrastructure.geocoding.MapTilerGeocodingClient;

class GeocodingServiceTest {

    private MapTilerGeocodingClient geocodingClient;
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        geocodingClient = mock(MapTilerGeocodingClient.class);
        geocodingService = new GeocodingService(geocodingClient);
    }

    @Test
    void returnsEmptyListForQueryThatIsTooShort() {
        List < Place > result = geocodingService.searchPlaces("a", 5);

        assertEquals(List.of(), result);
        verify(geocodingClient, never()).searchPlaces("a", 5);
    }

    @Test
    void returnsEmptyListForQueryThatIsTooLong() {
        String query = "a".repeat(201);

        List < Place > result = geocodingService.searchPlaces(query, 5);

        assertEquals(List.of(), result);
        verify(geocodingClient, never()).searchPlaces(query, 5);
    }

    @Test
    void trimsQueryAndUsesDefaultLimit() {
        Place place = new Place(
                "stuttgart",
                "Stuttgart, Deutschland",
                48.7758,
                9.1829
        );

        when(geocodingClient.searchPlaces("Stuttgart", 5))
                .thenReturn(List.of(place));

        List < Place > result =
                geocodingService.searchPlaces("  Stuttgart  ", null);

        assertEquals(List.of(place), result);
        verify(geocodingClient).searchPlaces("Stuttgart", 5);
    }

    @Test
    void limitsResultCountToMaximumTen() {
        when(geocodingClient.searchPlaces("Stuttgart", 10))
                .thenReturn(List.of());

        geocodingService.searchPlaces("Stuttgart", 20);

        verify(geocodingClient).searchPlaces("Stuttgart", 10);
    }
}