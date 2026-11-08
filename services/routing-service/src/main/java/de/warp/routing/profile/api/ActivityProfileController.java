// [KI-Log Nr. KI-062] (1b34cbd) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add central activity profile configuration (WARP-033)
// [KI-Log Nr. KI-073] (7a7605a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.profile.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.warp.routing.profile.application.ActivityProfileService;
import de.warp.routing.profile.domain.ActivityProfileCode;

@RestController
@RequestMapping("/internal/v1/activity-profiles")
public class ActivityProfileController {
    private final ActivityProfileService activityProfileService;

    public ActivityProfileController(
            ActivityProfileService activityProfileService
    ) {
        this.activityProfileService = activityProfileService;
    }

    @GetMapping
    public List<ActivityProfileResponse> getActivityProfiles() {
        return activityProfileService.getAllProfiles()
                .stream()
                .map(ActivityProfileResponse::from)
                .toList();
    }

    @GetMapping("/{code}")
    public ActivityProfileResponse getActivityProfile(
            @PathVariable ActivityProfileCode code
    ) {
        return ActivityProfileResponse.from(
                activityProfileService.getProfile(code)
        );
    }
}