// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-123] (00e2e64) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-137] (15d143f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.geocoding;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
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
            PlaceSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/places")
                            .queryParam("query", query)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(PlaceSearchResponse.class);

            if (response == null || response.items() == null) {
                return List.of();
            }

            return response.items();
        } catch (RestClientException exception) {
            throw new GeocodingServiceUnavailableException(
                    "Der Geocoding-Dienst ist nicht verfügbar.",
                    exception
            );
        }
    }
}