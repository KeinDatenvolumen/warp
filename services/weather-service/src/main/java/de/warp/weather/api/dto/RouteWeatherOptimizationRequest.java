// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
package de.warp.weather.api.dto;

import de.warp.weather.api.EvaluateRouteWeatherRequest.RouteSnapshotRequest;
import de.warp.weather.domain.model.RouteSnapshot;

import java.time.Instant;

public record RouteWeatherOptimizationRequest(
        String requestedByUserId,
        RouteSnapshotRequest routeSnapshot,
        Instant windowStart,
        Instant windowEnd
) {
    public RouteSnapshot toDomain() {
        if (routeSnapshot == null) {
            throw new IllegalArgumentException("routeSnapshot must not be null");
        }
        return routeSnapshot.toDomain();
    }
}
