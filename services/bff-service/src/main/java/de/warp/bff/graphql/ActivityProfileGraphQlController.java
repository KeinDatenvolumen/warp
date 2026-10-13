package de.warp.bff.graphql;

import java.util.List;

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

    public record ActivityProfilePayload(
            ActivityProfileCode code,
            String displayName,
            double averageSpeedKmh,
            RoutingProfileCode routingProfile,
            int weatherQueryIntervalMinutes,
            WayPreferencesPayload wayPreferences
    ) {

        private static ActivityProfilePayload from(
                ActivityProfileResponse response
        ) {
            return new ActivityProfilePayload(
                    ActivityProfileCode.valueOf(response.code()),
                    response.displayName(),
                    response.averageSpeedKmh(),
                    RoutingProfileCode.valueOf(
                            response.routingProfile()
                    ),
                    response.weatherQueryIntervalMinutes(),
                    WayPreferencesPayload.from(
                            response.wayPreferences()
                    )
            );
        }
    }

    public record WayPreferencesPayload(
            boolean preferCycleways,
            boolean avoidMotorways,
            boolean avoidTollRoads,
            boolean avoidUnpavedWays,
            boolean preferPavedWays,
            boolean preferScenicWays
    ) {

        private static WayPreferencesPayload from(
                ActivityProfileResponse.WayPreferencesResponse response
        ) {
            return new WayPreferencesPayload(
                    response.preferCycleways(),
                    response.avoidMotorways(),
                    response.avoidTollRoads(),
                    response.avoidUnpavedWays(),
                    response.preferPavedWays(),
                    response.preferScenicWays()
            );
        }
    }

    public enum ActivityProfileCode {
        BICYCLE,
        MOTORCYCLE,
        HIKING
    }

    public enum RoutingProfileCode {
        BICYCLE,
        MOTORCYCLE,
        FOOT
    }
}