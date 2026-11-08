// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-129] (d52e1bc) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve handling of first search and set min length to 3 in the routing service (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-139] (7de7075) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve handling of first search and set min length to 3 in the routing service (WARP-010)
// [KI-Log Nr. KI-236] (201cf90) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-035): add in-memory caching
// [KI-Log Nr. KI-237] (d313c4e) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-035 improve code, implement best practises
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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