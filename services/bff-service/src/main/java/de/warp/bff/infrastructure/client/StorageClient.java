// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.SavedRouteNotFoundException;
import de.warp.bff.application.exception.StorageServiceUnavailableException;
import de.warp.bff.application.exception.StorageValidationException;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.CreateSavedRouteRequest;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoute;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.StorageFieldError;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.StorageProblem;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.UpdateSavedRouteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class StorageClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StorageClient.class);

    private static final String SAVED_ROUTES_PATH =
            "/internal/v1/users/{userId}/saved-routes";

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final String IDEMPOTENCY_KEY_HEADER =
            "Idempotency-Key";

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
                .header(USER_ID_HEADER, userId)
                .retrieve()
                .body(SavedRoutePage.class));
    }

    public SavedRoute getSavedRoute(
            String userId,
            String routeId
    ) {
        return execute(() -> restClient
                .get()
                .uri(
                        SAVED_ROUTES_PATH + "/{routeId}",
                        userId,
                        routeId
                )
                .header(USER_ID_HEADER, userId)
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
                .uri(
                        SAVED_ROUTES_PATH + "/{routeId}",
                        userId,
                        routeId
                )
                .header(USER_ID_HEADER, userId)
                .body(request)
                .retrieve()
                .body(SavedRoute.class));
    }

    public void deleteSavedRoute(
            String userId,
            String routeId
    ) {
        execute(() -> {
            restClient
                    .delete()
                    .uri(
                            SAVED_ROUTES_PATH + "/{routeId}",
                            userId,
                            routeId
                    )
                    .header(USER_ID_HEADER, userId)
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
                .header(USER_ID_HEADER, userId)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .body(request)
                .retrieve()
                .body(SavedRoute.class));
    }

    private <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException exception) {
            int status = exception.getStatusCode().value();

            if (status == 400) {
                LOGGER.warn("Storage-Service meldete syntaktisch fehlerhafte Anfrage (400)");
                throw new IllegalArgumentException(
                        "Die Anfrage an den Speicherdienst ist ungültig.",
                        exception
                );
            }

            if (status == 401) {
                LOGGER.error("Interner Authentifizierungsfehler beim Aufruf des Storage-Services (401)");
                throw new StorageServiceUnavailableException(exception);
            }

            if (status == 403) {
                LOGGER.warn("Storage-Service verweigerte den Zugriff (403): {}", exception.getMessage());
                throw new SavedRouteNotFoundException(exception);
            }

            if (status == 404) {
                throw new SavedRouteNotFoundException(exception);
            }

            if (status == 409) {
                throw toValidationException(exception);
            }

            if (status == 422) {
                throw toValidationException(exception);
            }

            LOGGER.warn(
                    "Storage-Service antwortete mit Status {}",
                    status
            );

            throw new StorageServiceUnavailableException(exception);
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "Storage-Service antwortete mit Status {}",
                    exception.getStatusCode().value()
            );

            throw new StorageServiceUnavailableException(exception);
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Storage-Service konnte nicht erreicht werden.",
                    exception
            );

            throw new StorageServiceUnavailableException(exception);
        }
    }

    private StorageValidationException toValidationException(
            HttpClientErrorException exception
    ) {
        List<StorageFieldError> fieldErrors = List.of();

        try {
            StorageProblem problem =
                    exception.getResponseBodyAs(StorageProblem.class);

            if (problem != null && problem.fieldErrors() != null) {
                fieldErrors = problem.fieldErrors();
            }
        } catch (RuntimeException parsingException) {
            LOGGER.debug(
                    "Validierungsfehler des Storage-Service "
                            + "konnte nicht gelesen werden.",
                    parsingException
            );
        }

        return new StorageValidationException(
                fieldErrors,
                exception
        );
    }
}