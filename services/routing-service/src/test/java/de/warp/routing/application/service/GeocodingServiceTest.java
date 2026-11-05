// [KI-Log Nr. KI-22] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): In-Memory-Caching mit Caffeine (WARP-035)
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

class GeocodingServiceTest {

    private CachedGeocodingService cachedGeocodingService;
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        cachedGeocodingService =
                mock(CachedGeocodingService.class);

        geocodingService = new GeocodingService(
                cachedGeocodingService
        );
    }

    @Test
    void returnsEmptyListForNullQuery() {
        List<Place> result =
                geocodingService.searchPlaces(null, 5);

        assertEquals(List.of(), result);

        verify(cachedGeocodingService, never())
                .searchPlaces(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyInt()
                );
    }

    @Test
    void returnsEmptyListForQueryThatIsTooShort() {
        List<Place> result =
                geocodingService.searchPlaces("ab", 5);

        assertEquals(List.of(), result);

        verify(cachedGeocodingService, never())
                .searchPlaces(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyInt()
                );
    }

    @Test
    void returnsEmptyListForQueryThatIsTooLong() {
        String query = "a".repeat(201);

        List<Place> result =
                geocodingService.searchPlaces(query, 5);

        assertEquals(List.of(), result);

        verify(cachedGeocodingService, never())
                .searchPlaces(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyInt()
                );
    }

    @Test
    void trimsAndLowercasesQueryAndUsesDefaultLimit() {
        Place place = new Place(
                "stuttgart",
                "Stuttgart, Deutschland",
                48.7758,
                9.1829
        );

        when(cachedGeocodingService.searchPlaces(
                "stuttgart",
                5
        )).thenReturn(List.of(place));

        List<Place> result =
                geocodingService.searchPlaces(
                        "  Stuttgart  ",
                        null
                );

        assertEquals(List.of(place), result);

        verify(cachedGeocodingService)
                .searchPlaces("stuttgart", 5);
    }

    @Test
    void convertsQueryToLowerCase() {
        when(cachedGeocodingService.searchPlaces(
                "berlin",
                5
        )).thenReturn(List.of());

        geocodingService.searchPlaces("BeRLiN", 5);

        verify(cachedGeocodingService)
                .searchPlaces("berlin", 5);
    }

    @Test
    void limitsResultCountToMaximumTen() {
        when(cachedGeocodingService.searchPlaces(
                "stuttgart",
                10
        )).thenReturn(List.of());

        geocodingService.searchPlaces("Stuttgart", 20);

        verify(cachedGeocodingService)
                .searchPlaces("stuttgart", 10);
    }

    @Test
    void limitsResultCountToMinimumOne() {
        when(cachedGeocodingService.searchPlaces(
                "stuttgart",
                1
        )).thenReturn(List.of());

        geocodingService.searchPlaces("Stuttgart", 0);

        verify(cachedGeocodingService)
                .searchPlaces("stuttgart", 1);
    }
}