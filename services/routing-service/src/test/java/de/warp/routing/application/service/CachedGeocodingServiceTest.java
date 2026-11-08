// [KI-Log Nr. KI-236] (201cf90) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-035): add in-memory caching
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.application.service;

import de.warp.routing.domain.model.Place;
import de.warp.routing.infrastructure.geocoding.MapTilerGeocodingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedGeocodingServiceTest {

    private AnnotationConfigApplicationContext context;
    private MapTilerGeocodingClient geocodingClient;
    private CachedGeocodingService cachedGeocodingService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(
                TestConfiguration.class
        );

        geocodingClient = context.getBean(
                MapTilerGeocodingClient.class
        );

        cachedGeocodingService = context.getBean(
                CachedGeocodingService.class
        );

        CacheManager cacheManager =
                context.getBean(CacheManager.class);

        var cache = cacheManager.getCache(
                CachedGeocodingService.CACHE_NAME
        );

        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void cachesSuccessfulGeocodingResult() {
        Place place = new Place(
                "stuttgart",
                "Stuttgart, Deutschland",
                48.7758,
                9.1829
        );

        when(geocodingClient.searchPlaces("Stuttgart", 5))
                .thenReturn(List.of(place));

        List<Place> firstResult =
                cachedGeocodingService.searchPlaces(
                        "Stuttgart",
                        5
                );

        List<Place> secondResult =
                cachedGeocodingService.searchPlaces(
                        "Stuttgart",
                        5
                );

        assertThat(firstResult).containsExactly(place);
        assertThat(secondResult).containsExactly(place);

        verify(geocodingClient, times(1))
                .searchPlaces("Stuttgart", 5);
    }

    @Test
    void usesLimitAsPartOfCacheKey() {
        when(geocodingClient.searchPlaces("Stuttgart", 5))
                .thenReturn(List.of());

        when(geocodingClient.searchPlaces("Stuttgart", 10))
                .thenReturn(List.of());

        cachedGeocodingService.searchPlaces(
                "Stuttgart",
                5
        );

        cachedGeocodingService.searchPlaces(
                "Stuttgart",
                10
        );

        verify(geocodingClient, times(1))
                .searchPlaces("Stuttgart", 5);

        verify(geocodingClient, times(1))
                .searchPlaces("Stuttgart", 10);
    }

    @Test
    void doesNotCacheExceptions() {
        when(geocodingClient.searchPlaces("Stuttgart", 5))
                .thenThrow(new IllegalStateException(
                        "Geocoding nicht verfügbar"
                ))
                .thenReturn(List.of());

        assertThatThrownBy(
                () -> cachedGeocodingService.searchPlaces(
                        "Stuttgart",
                        5
                )
        ).isInstanceOf(IllegalStateException.class);

        assertThat(
                cachedGeocodingService.searchPlaces(
                        "Stuttgart",
                        5
                )
        ).isEmpty();

        verify(geocodingClient, times(2))
                .searchPlaces("Stuttgart", 5);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class TestConfiguration {

        @Bean
        MapTilerGeocodingClient geocodingClient() {
            return mock(MapTilerGeocodingClient.class);
        }

        @Bean
        CachedGeocodingService cachedGeocodingService(
                MapTilerGeocodingClient geocodingClient
        ) {
            return new CachedGeocodingService(
                    geocodingClient
            );
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CachedGeocodingService.CACHE_NAME
            );
        }
    }
}