// [KI-Log Nr. KI-101] (c1e1a5c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-133] (943d006) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import de.warp.bff.domain.model.Place;
import de.warp.bff.infrastructure.geocoding.RoutingGeocodingClient;

@Service
public class GeocodingService {

    private static final int MINIMUM_QUERY_LENGTH = 3;
    private static final int MAXIMUM_QUERY_LENGTH = 200;
    private static final int MINIMUM_LIMIT = 1;
    private static final int MAXIMUM_LIMIT = 10;

    private final RoutingGeocodingClient geocodingClient;

    public GeocodingService(
            RoutingGeocodingClient geocodingClient
    ) {
        this.geocodingClient = geocodingClient;
    }

    public List<Place> searchPlaces(String query, Integer limit) {
        if (query == null
                || query.trim().length() < MINIMUM_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Der Suchbegriff muss mindestens drei Zeichen enthalten."
            );
        }

        String normalizedQuery = query.trim();

        if (normalizedQuery.length() > MAXIMUM_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Der Suchbegriff darf maximal 200 Zeichen enthalten."
            );
        }

        int effectiveLimit = limit == null ? 5 : limit;

        if (effectiveLimit < MINIMUM_LIMIT
                || effectiveLimit > MAXIMUM_LIMIT) {
            throw new IllegalArgumentException(
                    "Das Ergebnislimit muss zwischen 1 und 10 liegen."
            );
        }

        return geocodingClient.searchPlaces(
                normalizedQuery,
                effectiveLimit
        );
    }
}