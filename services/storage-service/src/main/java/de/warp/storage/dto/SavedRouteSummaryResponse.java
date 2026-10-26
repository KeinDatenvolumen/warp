package de.warp.storage.dto;

import java.time.OffsetDateTime;

public record SavedRouteSummaryResponse(
        String id,
        String name,
        PlaceDto start,
        PlaceDto destination,
        String activityProfileCode,
        int weatherScore,
        boolean favorite,
        OffsetDateTime createdAt
) {
}