package de.warp.routing.route.api;

import java.time.Instant;

public record StartRouteCalculationRequest(
        String requestedByUserId,
        RoutePointRequest start,
        RoutePointRequest destination,
        Instant departureTime,
        ActivityProfileSnapshotRequest activityProfile
) {
}