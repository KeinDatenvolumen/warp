package de.warp.routing.profile.application;

import java.util.List;

import org.springframework.stereotype.Service;

import de.warp.routing.profile.domain.ActivityProfile;
import de.warp.routing.profile.domain.ActivityProfileCode;
import de.warp.routing.profile.domain.RoutingProfileCode;
import de.warp.routing.profile.domain.WayPreferences;

@Service
public class ActivityProfileService {

    private static final List<ActivityProfile> ACTIVITY_PROFILES = List.of(
            new ActivityProfile(
                    ActivityProfileCode.BICYCLE,
                    "Fahrrad",
                    18.0,
                    RoutingProfileCode.BICYCLE,
                    15,
                    new WayPreferences(
                            true,
                            true,
                            true,
                            false,
                            true,
                            true
                    )
            ),
            new ActivityProfile(
                    ActivityProfileCode.MOTORCYCLE,
                    "Motorrad",
                    60.0,
                    RoutingProfileCode.MOTORCYCLE,
                    30,
                    new WayPreferences(
                            false,
                            false,
                            false,
                            true,
                            true,
                            true
                    )
            ),
            new ActivityProfile(
                    ActivityProfileCode.HIKING,
                    "Wandern",
                    5.0,
                    RoutingProfileCode.FOOT,
                    10,
                    new WayPreferences(
                            false,
                            true,
                            true,
                            false,
                            true,
                            true
                    )
            )
    );

    public List<ActivityProfile> getAllProfiles() {
        return ACTIVITY_PROFILES;
    }

    public ActivityProfile getProfile(ActivityProfileCode code) {
        return ACTIVITY_PROFILES.stream()
                .filter(profile -> profile.code() == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekanntes Aktivitätsprofil: " + code
                ));
    }
}