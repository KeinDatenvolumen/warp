package de.warp.bff.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import de.warp.bff.domain.model.Place;
import de.warp.bff.infrastructure.geocoding.MapTilerGeocodingClient;

@Service
public class GeocodingService {

    private static final int MINIMUM_QUERY_LENGTH = 3;
    private static final int MINIMUM_LIMIT = 1;
    private static final int MAXIMUM_LIMIT = 10;

    private final MapTilerGeocodingClient geocodingClient;

    public GeocodingService(
            MapTilerGeocodingClient geocodingClient
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

        int effectiveLimit = limit == null ? 5 : limit;

        if (effectiveLimit < MINIMUM_LIMIT
                || effectiveLimit > MAXIMUM_LIMIT) {
            throw new IllegalArgumentException(
                    "Das Ergebnislimit muss zwischen 1 und 10 liegen."
            );
        }

        return geocodingClient.searchPlaces(
                query.trim(),
                effectiveLimit
        );
    }
}