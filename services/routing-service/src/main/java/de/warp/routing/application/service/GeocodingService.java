package de.warp.routing.application.service;

import java.util.List;

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

        String normalizedQuery = query.trim();

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