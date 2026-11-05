// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
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