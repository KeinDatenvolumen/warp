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

    @Cacheable(cacheNames = CACHE_NAME)
    public List<Place> searchPlaces(
            String query,
            int limit
    ) {
        return geocodingClient.searchPlaces(query, limit);
    }
}