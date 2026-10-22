package de.warp.routing.route.api;

public record RouteCalculationErrorResponse(
        String code,
        String message
) {
}