// [KI-Log Nr. KI-063] (cde9f97) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose activity profiles through BFF (WARP-033)
// [KI-Log Nr. KI-065] (73f94e9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add activity profile selection to route planner (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-161] (078f95b) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose routing engine profile in activity profiles
// [KI-Log Nr. KI-177] (c0720fe) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose routing engine profile in activity profiles
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivityProfileResponse(
        String code,
        String displayName,
        double averageSpeedKmh,
        String routingProfile,
        Integer weatherQueryIntervalKm,
        Integer weatherQueryIntervalMinutes,
        WayPreferencesResponse wayPreferences
) {

    public ActivityProfileResponse(
            String code,
            String displayName,
            double averageSpeedKmh,
            String routingProfile,
            int weatherQueryInterval,
            WayPreferencesResponse wayPreferences
    ) {
        this(
                code,
                displayName,
                averageSpeedKmh,
                routingProfile,
                weatherQueryInterval,
                weatherQueryInterval,
                wayPreferences
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
    }
}