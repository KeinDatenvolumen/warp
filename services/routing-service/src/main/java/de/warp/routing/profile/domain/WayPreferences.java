// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
package de.warp.routing.profile.domain;

public record WayPreferences(
        boolean preferCycleways,
        boolean avoidMotorways,
        boolean avoidTollRoads,
        boolean avoidUnpavedWays,
        boolean preferPavedWays,
        boolean preferScenicWays
) {
}