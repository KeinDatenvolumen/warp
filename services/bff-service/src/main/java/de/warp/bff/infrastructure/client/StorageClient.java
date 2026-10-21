package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.SavedRouteNotFoundException;
import de.warp.bff.application.exception.StorageServiceUnavailableException;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.CreateSavedRouteRequest;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoute;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.UpdateSavedRouteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.function.Supplier;

@Component
public class StorageClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StorageClient.class);

    private static final String SAVED_ROUTES_PATH =
            "/internal/v1/users/{userId}/saved-routes";

    private final RestClient restClient;

    public StorageClient(
            @Qualifier("storageRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public SavedRoutePage listSavedRoutes(
            String userId,
            Boolean favorite,
            int page,
            int size
    ) {
        return execute(() -> restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(SAVED_ROUTES_PATH)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParamIfPresent(
                                "favorite",
                                Optional.ofNullable(favorite)
                        )
                        .build(userId))
                .retrieve()
                .body(SavedRoutePage.class));
    }

    public SavedRoute getSavedRoute(String userId, String routeId) {
        return execute(() -> restClient
                .get()
                .uri(SAVED_ROUTES_PATH + "/{routeId}", userId, routeId)
                .retrieve()
                .body(SavedRoute.class));
    }

    public SavedRoute updateSavedRoute(
            String userId,
            String routeId,
            UpdateSavedRouteRequest request
    ) {
        return execute(() -> restClient
                .patch()
                .uri(SAVED_ROUTES_PATH + "/{routeId}", userId, routeId)
                .body(request)
                .retrieve()
                .body(SavedRoute.class));
    }

    public void deleteSavedRoute(String userId, String routeId) {
        execute(() -> {
            restClient
                    .delete()
                    .uri(SAVED_ROUTES_PATH + "/{routeId}", userId, routeId)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    public SavedRoute createSavedRoute(
            String userId,
            String idempotencyKey,
            CreateSavedRouteRequest request
    ) {
        return execute(() -> restClient
                .post()
                .uri(SAVED_ROUTES_PATH, userId)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .body(SavedRoute.class));
    }

    private <T> T execute(Supplier<T> call) {
        try {
            T result = call.get();
            return result;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new SavedRouteNotFoundException(exception);
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "Storage-Service antwortete mit Status {}",
                    exception.getStatusCode().value()
            );
            throw new StorageServiceUnavailableException(exception);
        } catch (RestClientException exception) {
            throw new StorageServiceUnavailableException(exception);
        }
    }
}