// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-129] (d52e1bc) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve handling of first search and set min length to 3 in the routing service (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-139] (7de7075) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve handling of first search and set min length to 3 in the routing service (WARP-010)
// [KI-Log Nr. KI-236] (201cf90) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-035): add in-memory caching
// [KI-Log Nr. KI-237] (d313c4e) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-035 improve code, implement best practises
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.application.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import de.warp.routing.domain.model.Place;

@Service
public class GeocodingService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAXIMUM_LIMIT = 10;
    private static final int MINIMUM_QUERY_LENGTH = 3;
    private static final int MAXIMUM_QUERY_LENGTH = 200;

    private final CachedGeocodingService cachedGeocodingService;

    public GeocodingService(
            CachedGeocodingService cachedGeocodingService
    ) {
        this.cachedGeocodingService = cachedGeocodingService;
    }

    public List<Place> searchPlaces(
            String query,
            Integer limit
    ) {
        if (query == null) {
            return List.of();
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

        if (normalizedQuery.length() < MINIMUM_QUERY_LENGTH
                || normalizedQuery.length() > MAXIMUM_QUERY_LENGTH) {
            return List.of();
        }

        int effectiveLimit = limit == null
                ? DEFAULT_LIMIT
                : Math.clamp(limit, 1, MAXIMUM_LIMIT);

        return cachedGeocodingService.searchPlaces(
                normalizedQuery,
                effectiveLimit
        );
    }
}