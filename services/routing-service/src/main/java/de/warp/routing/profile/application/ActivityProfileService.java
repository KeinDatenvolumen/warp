// [KI-Log Nr. KI-062] (1b34cbd) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add central activity profile configuration (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
                    5,
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
                    15,
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
                    2,
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