// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.api.dto;

import de.warp.weather.api.EvaluateRouteWeatherRequest.RouteSnapshotRequest;
import de.warp.weather.domain.model.RouteSnapshot;

public record RouteWeatherAiAnalysisRequest(
        String requestedByUserId,
        String startLabel,
        String destinationLabel,
        RouteSnapshotRequest routeSnapshot
) {
    public RouteSnapshot toDomain() {
        if (routeSnapshot == null) {
            throw new IllegalArgumentException("routeSnapshot must not be null");
        }
        return routeSnapshot.toDomain();
    }
}
