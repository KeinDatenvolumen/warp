package de.warp.bff.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import de.warp.bff.api.dto.ActivityProfileResponse;
import de.warp.bff.application.service.ActivityProfileService;

@Controller
public class ActivityProfileGraphQlController {

    private final ActivityProfileService activityProfileService;

    public ActivityProfileGraphQlController(
            ActivityProfileService activityProfileService
    ) {
        this.activityProfileService = activityProfileService;
    }

    @QueryMapping
    public List<ActivityProfilePayload> activityProfiles() {
        return activityProfileService.getActivityProfiles()
                .stream()
                .map(ActivityProfilePayload::from)
                .toList();
    }

    @QueryMapping
    public ActivityProfilePayload activityProfile(
            @Argument ActivityProfileCode code
    ) {
        return ActivityProfilePayload.from(
                activityProfileService.getActivityProfile(code.name())
        );
    }

    public record ActivityProfilePayload(
            ActivityProfileCode code,
            String displayName
    ) {

        private static ActivityProfilePayload from(
                ActivityProfileResponse response
        ) {
            return new ActivityProfilePayload(
                    ActivityProfileCode.valueOf(response.code()),
                    response.displayName()
            );
        }
    }

    public enum ActivityProfileCode {
        BICYCLE,
        MOTORCYCLE,
        HIKING
    }
}