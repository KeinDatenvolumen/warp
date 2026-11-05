// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
package de.warp.routing.route.api;

import de.warp.routing.profile.domain.ActivityProfileCode;

public record ActivityProfileSnapshotRequest(
        ActivityProfileCode code,
        String routingProfile
) {
}