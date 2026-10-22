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