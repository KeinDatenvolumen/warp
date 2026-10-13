package de.warp.routing.profile.domain;

public record ActivityProfile(
        ActivityProfileCode code,
        String displayName,
        double averageSpeedKmh,
        RoutingProfileCode routingProfile,
        int weatherQueryIntervalMinutes,
        WayPreferences wayPreferences
) {
}