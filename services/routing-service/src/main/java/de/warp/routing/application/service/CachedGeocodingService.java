// [KI-Log Nr. KI-236] (201cf90) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-035): add in-memory caching
// [KI-Log Nr. KI-237] (d313c4e) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-035 improve code, implement best practises
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.application.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import de.warp.routing.domain.model.Place;
import de.warp.routing.infrastructure.geocoding.MapTilerGeocodingClient;

@Service
public class CachedGeocodingService {

    public static final String CACHE_NAME =
            "geocoding-results";

    private final MapTilerGeocodingClient geocodingClient;

    public CachedGeocodingService(
            MapTilerGeocodingClient geocodingClient
    ) {
        this.geocodingClient = geocodingClient;
    }

    @Cacheable(cacheNames = CACHE_NAME, sync = true)
    public List<Place> searchPlaces(
            String query,
            int limit
    ) {
        return geocodingClient.searchPlaces(query, limit);
    }
}