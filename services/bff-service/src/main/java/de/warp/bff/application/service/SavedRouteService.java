package de.warp.bff.application.service;

import de.warp.bff.infrastructure.client.StorageClient;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoute;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.UpdateSavedRouteRequest;
import org.springframework.stereotype.Service;

@Service
public class SavedRouteService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final StorageClient storageClient;

    public SavedRouteService(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    public SavedRoutePage listSavedRoutes(
            String userId,
            Boolean favorite,
            Integer page,
            Integer size
    ) {
        int effectivePage = page == null ? 0 : page;
        int effectiveSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (effectivePage < 0
                || effectiveSize < 1
                || effectiveSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Ungültige Seitenparameter."
            );
        }

        return storageClient.listSavedRoutes(
                userId,
                favorite,
                effectivePage,
                effectiveSize
        );
    }

    public SavedRoute getSavedRoute(String userId, String routeId) {
        return storageClient.getSavedRoute(userId, routeId);
    }

    public SavedRoute updateSavedRoute(
            String userId,
            String routeId,
            String name,
            Boolean favorite
    ) {
        return storageClient.updateSavedRoute(
                userId,
                routeId,
                new UpdateSavedRouteRequest(name, favorite)
        );
    }

    public void deleteSavedRoute(String userId, String routeId) {
        storageClient.deleteSavedRoute(userId, routeId);
    }
}