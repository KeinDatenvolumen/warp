package de.warp.storage.dto;

import java.util.List;

public record SavedRoutePageResponse(
        List<SavedRouteSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}