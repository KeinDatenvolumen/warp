package de.warp.bff.api.dto;

public record ActivityProfileResponse(
        String code,
        String displayName
        String displayName,
        double averageSpeedKmh,
        String routingProfile,
        int weatherQueryIntervalMinutes,
        WayPreferencesResponse wayPreferences
) {

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