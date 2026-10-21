package de.warp.routing.infrastructure.routing;

import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenRouteServiceClient {

    private static final Set<String> SUPPORTED_ROUTING_PROFILES =
            Set.of(
                    "cycling-regular",
                    "driving-car",
                    "foot-hiking"
            );

    private final RestClient restClient;
    private final String apiKey;

    public OpenRouteServiceClient(
            @Qualifier("openRouteServiceRestClient")
            RestClient restClient,
            @Value("${warp.routing.openrouteservice.api-key}")
            String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public OpenRouteServiceResponse.Feature calculateRoute(
            String routingProfile,
            double startLatitude,
            double startLongitude,
            double destinationLatitude,
            double destinationLongitude
    ) {
        validateApiKey();
        validateRoutingProfile(routingProfile);

        OpenRouteServiceRequest request =
                OpenRouteServiceRequest.between(
                        startLatitude,
                        startLongitude,
                        destinationLatitude,
                        destinationLongitude
                );

        try {
            OpenRouteServiceResponse response =
                    restClient.post()
                            .uri(
                                    "/v2/directions/{profile}/geojson",
                                    routingProfile
                            )
                            .header("Authorization", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(OpenRouteServiceResponse.class);

            return extractRoute(response);
        } catch (RestClientException exception) {
            throw new RoutingServiceUnavailableException(
                    "Der externe Routing-Dienst ist nicht verfügbar.",
                    exception
            );
        }
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RoutingServiceUnavailableException(
                    "Der OpenRouteService API-Key ist nicht konfiguriert."
            );
        }
    }

    private void validateRoutingProfile(String routingProfile) {
        if (routingProfile == null
                || routingProfile.isBlank()
                || !SUPPORTED_ROUTING_PROFILES.contains(routingProfile)) {
            throw new IllegalArgumentException(
                    "Das Routingprofil wird nicht unterstützt."
            );
        }
    }

    private OpenRouteServiceResponse.Feature extractRoute(
            OpenRouteServiceResponse response
    ) {
        if (response == null
                || response.features() == null
                || response.features().isEmpty()) {
            throw new RouteNotFoundException(
                    "Es konnte keine Route gefunden werden."
            );
        }

        OpenRouteServiceResponse.Feature route =
                response.features().getFirst();

        if (route == null
                || route.geometry() == null
                || route.geometry().coordinates() == null
                || route.geometry().coordinates().isEmpty()
                || route.properties() == null
                || route.properties().summary() == null) {
            throw new RoutingServiceUnavailableException(
                    "Der externe Routing-Dienst hat eine "
                            + "unvollständige Antwort geliefert."
            );
        }

        return route;
    }
}