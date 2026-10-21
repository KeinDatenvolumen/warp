package de.warp.storage.dto;

import java.time.OffsetDateTime;

public record SavedRouteResponse(
        String id,
        String name,
        boolean favorite,
        RouteSnapshotDto routeSnapshot,
        WeatherSnapshotDto weatherSnapshot,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}