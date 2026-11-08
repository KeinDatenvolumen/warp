// [KI-Log Nr. KI-101] (c1e1a5c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-133] (943d006) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.infrastructure.geocoding;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import de.warp.routing.domain.model.Place;

@Component
public class MapTilerGeocodingClient {

    private final RestClient restClient;
    private final String apiKey;

    public MapTilerGeocodingClient(
            @Qualifier("mapTilerRestClient") RestClient restClient,
            @Value("${warp.geocoding.maptiler.api-key}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public List<Place> searchPlaces(String query, int limit) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeocodingServiceUnavailableException(
                    "Der MapTiler API-Key ist nicht konfiguriert."
            );
        }

        try {
            MapTilerGeocodingResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("geocoding", query + ".json")
                            .queryParam("key", apiKey)
                            .queryParam("limit", limit)
                            .queryParam("language", "de")
                            .queryParam("autocomplete", true)
                            .build())
                    .retrieve()
                    .body(MapTilerGeocodingResponse.class);

            if (response == null || response.features() == null) {
                return List.of();
            }

            return response.features().stream()
                    .map(this::toPlace)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (RestClientException exception) {
            throw new GeocodingServiceUnavailableException(
                    "Der Geocoding-Dienst ist nicht verfügbar.",
                    exception
            );
        }
    }

    Place toPlace(MapTilerGeocodingResponse.Feature feature) {
        if (feature == null
                || feature.id() == null
                || feature.id().isBlank()
                || feature.placeName() == null
                || feature.placeName().isBlank()
                || feature.center() == null
                || feature.center().size() < 2
                || feature.center().get(0) == null
                || feature.center().get(1) == null) {
            return null;
        }

        double longitude = feature.center().get(0);
        double latitude = feature.center().get(1);

        if (!Double.isFinite(longitude)
                || !Double.isFinite(latitude)
                || longitude < -180
                || longitude > 180
                || latitude < -90
                || latitude > 90) {
            return null;
        }

        return new Place(
                feature.id(),
                feature.placeName(),
                latitude,
                longitude
        );
    }
}