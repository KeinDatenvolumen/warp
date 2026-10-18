package de.warp.routing.domain.model;

public record Place(
        String placeId,
        String displayName,
        double latitude,
        double longitude
) {
}