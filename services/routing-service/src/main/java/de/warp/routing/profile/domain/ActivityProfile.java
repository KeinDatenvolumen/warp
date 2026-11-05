// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
package de.warp.routing.profile.domain;

public record ActivityProfile(
        ActivityProfileCode code,
        String displayName,
        double averageSpeedKmh,
        RoutingProfileCode routingProfile,
        int weatherQueryIntervalKm,
        WayPreferences wayPreferences
) {
}