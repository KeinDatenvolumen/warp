package de.warp.routing.profile.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.warp.routing.profile.application.ActivityProfileService;
import de.warp.routing.profile.domain.ActivityProfileCode;

@RestController
@RequestMapping("/internal/activity-profiles")
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