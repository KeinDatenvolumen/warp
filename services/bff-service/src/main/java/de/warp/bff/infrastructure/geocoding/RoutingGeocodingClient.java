package de.warp.bff.infrastructure.geocoding;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import de.warp.bff.domain.model.Place;

@Component
public class RoutingGeocodingClient {

    private final RestClient restClient;

    public RoutingGeocodingClient(
            @Qualifier("routingGeocodingRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public List < Place > searchPlaces(
            String query,
            int limit
    ) {
        try {
            List < Place > places = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/places")
                            .queryParam("query", query)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(
                            new ParameterizedTypeReference
                                    < List < Place > >() {
                            }
                    );

            if (places == null) {
                return List.of();
            }

            return places;
        } catch (RestClientException exception) {
            throw new GeocodingServiceUnavailableException(
                    "Der Geocoding-Dienst ist nicht verfügbar.",
                    exception
            );
        }
    }
}