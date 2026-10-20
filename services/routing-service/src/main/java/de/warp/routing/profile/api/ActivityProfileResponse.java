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
                profile.routingProfile().name(),
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