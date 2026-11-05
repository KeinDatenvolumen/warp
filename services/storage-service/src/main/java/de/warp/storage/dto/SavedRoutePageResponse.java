// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage.dto;

import java.util.List;

public record SavedRoutePageResponse(
        List<SavedRouteSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}