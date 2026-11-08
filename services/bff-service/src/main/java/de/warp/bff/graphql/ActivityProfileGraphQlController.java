// [KI-Log Nr. KI-063] (cde9f97) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose activity profiles through BFF (WARP-033)
// [KI-Log Nr. KI-065] (73f94e9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add activity profile selection to route planner (WARP-033)
// [KI-Log Nr. KI-074] (cfd4246) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose individual activity profile query (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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