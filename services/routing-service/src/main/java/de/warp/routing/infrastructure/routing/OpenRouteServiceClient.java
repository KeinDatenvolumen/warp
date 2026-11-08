// [KI-Log Nr. KI-154] (2dcf6bf) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-164] (ca6af77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 accept GeoJSON responses from OpenRouteService
// [KI-Log Nr. KI-170] (3b5d8a2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-180] (a8941b2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 accept GeoJSON responses from OpenRouteService
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.infrastructure.routing;

import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenRouteServiceClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    OpenRouteServiceClient.class
            );

    private static final MediaType GEOJSON_MEDIA_TYPE =
            MediaType.parseMediaType(
                    "application/geo+json"
            );

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
                                    "/openrouteservice/v2/directions/{profile}/geojson",
                                    routingProfile
                            )
                            .header("Authorization", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(
                                    GEOJSON_MEDIA_TYPE,
                                    MediaType.APPLICATION_JSON
                            )
                            .body(request)
                            .retrieve()
                            .body(OpenRouteServiceResponse.class);

            return extractRoute(response);
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "OpenRouteService antwortete mit HTTP-Status {}.",
                    exception.getStatusCode().value()
            );

            if (isRouteNotFound(exception)) {
                throw new RouteNotFoundException(
                        "Für die angegebenen Koordinaten konnte keine Route gefunden werden.",
                        exception
                );
            }

            throw new RoutingServiceUnavailableException(
                    "Der externe Routing-Dienst ist nicht verfügbar.",
                    exception
            );
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "OpenRouteService-Aufruf ist technisch fehlgeschlagen: {}",
                    exception.getClass().getSimpleName()
            );

            throw new RoutingServiceUnavailableException(
                    "Der externe Routing-Dienst ist nicht verfügbar.",
                    exception
            );
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "OpenRouteService-Antwort konnte nicht verarbeitet werden: {}",
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }

    private boolean isRouteNotFound(
            RestClientResponseException exception
    ) {
        int status = exception.getStatusCode().value();
        if (status == 404) {
            return true;
        }

        if (status == 400) {
            String body = exception.getResponseBodyAsString();
            if (!body.isBlank()) {
                String lower = body.toLowerCase();
                return lower.contains("could not find")
                        || lower.contains("cannot find")
                        || lower.contains("not found")
                        || body.contains("2004")
                        || body.contains("2010")
                        || body.contains("2099");
            }
        }

        return false;
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
                || route.properties().summary() == null
                || route.properties().segments() == null
                || route.properties().segments().isEmpty()) {
            throw new RoutingServiceUnavailableException(
                    "Der externe Routing-Dienst hat eine "
                            + "unvollständige Antwort geliefert."
            );
        }

        return route;
    }
}