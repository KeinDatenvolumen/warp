package de.warp.routing.route.api;

import de.warp.routing.profile.domain.ActivityProfileCode;

public record ActivityProfileSnapshotRequest(
        ActivityProfileCode code,
        String routingProfile
) {
}