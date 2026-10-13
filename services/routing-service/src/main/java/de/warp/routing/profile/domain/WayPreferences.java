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