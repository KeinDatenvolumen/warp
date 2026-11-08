// [KI-Log Nr. KI-062] (1b34cbd) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add central activity profile configuration (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-161] (078f95b) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose routing engine profile in activity profiles
// [KI-Log Nr. KI-177] (c0720fe) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose routing engine profile in activity profiles
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.profile.api;

import de.warp.routing.profile.domain.ActivityProfile;

public record ActivityProfileResponse(
        String code,
        String displayName,
        double averageSpeedKmh,
        String routingProfile,
        int weatherQueryIntervalKm,
        WayPreferencesResponse wayPreferences
) {

    public static ActivityProfileResponse from(
            ActivityProfile profile
    ) {
        return new ActivityProfileResponse(
                profile.code().name(),
                profile.displayName(),
                profile.averageSpeedKmh(),
                profile.routingProfile().externalCode(),
                profile.weatherQueryIntervalKm(),
                WayPreferencesResponse.from(profile)
        );
    }

    public record WayPreferencesResponse(
            boolean preferCycleways,
            boolean avoidMotorways,
            boolean avoidTollRoads,
            boolean avoidUnpavedWays,
            boolean preferPavedWays,
            boolean preferScenicWays
    ) {
        private static WayPreferencesResponse from(
                ActivityProfile profile
        ) {
            var preferences = profile.wayPreferences();

            return new WayPreferencesResponse(
                    preferences.preferCycleways(),
                    preferences.avoidMotorways(),
                    preferences.avoidTollRoads(),
                    preferences.avoidUnpavedWays(),
                    preferences.preferPavedWays(),
                    preferences.preferScenicWays()
            );
        }
    }
}