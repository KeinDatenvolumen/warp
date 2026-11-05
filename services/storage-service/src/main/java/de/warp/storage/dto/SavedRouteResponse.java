// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
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