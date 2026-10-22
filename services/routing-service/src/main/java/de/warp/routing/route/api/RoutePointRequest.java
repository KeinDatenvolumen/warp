package de.warp.routing.route.api;

public record RoutePointRequest(
        String label,
        double latitude,
        double longitude
) {
}