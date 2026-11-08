// [KI-Log Nr. KI-063] (cde9f97) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose activity profiles through BFF (WARP-033)
// [KI-Log Nr. KI-073] (7a7605a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-033)
// [KI-Log Nr. KI-074] (cfd4246) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose individual activity profile query (WARP-033)
// [KI-Log Nr. KI-143] (2e513c7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: rest client configuration (WARP-033)
// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Qualifier;

import de.warp.bff.api.dto.ActivityProfileResponse;

@Service
public class ActivityProfileService {

    private final RestClient restClient;
    private final String routingServiceUrl;

    public ActivityProfileService(
            @Qualifier("serviceHealthRestClient")
            RestClient restClient,
            @Value("${warp.services.routing.url}")
            String routingServiceUrl
    ) {
        this.restClient = restClient;
        this.routingServiceUrl = removeTrailingSlash(routingServiceUrl);
    }

    public List<ActivityProfileResponse> getActivityProfiles() {
        try {
            var response = restClient
                    .get()
                    .uri(routingServiceUrl + "/internal/v1/activity-profiles")
                    .retrieve()
                    .body(ActivityProfileResponse[].class);

            if (response == null) {
                throw new IllegalStateException(
                        "Routing-Service lieferte keine Aktivitätsprofile."
                );
            }

            return List.of(response);
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Aktivitätsprofile konnten nicht geladen werden.",
                    exception
            );
        }
    }

    public ActivityProfileResponse getActivityProfile(String code) {
        try {
            var response = restClient
                    .get()
                    .uri(
                            routingServiceUrl
                                    + "/internal/v1/activity-profiles/"
                                    + code
                    )
                    .retrieve()
                    .body(ActivityProfileResponse.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Routing-Service lieferte kein Aktivitätsprofil."
                );
            }

            return response;
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Aktivitätsprofil konnte nicht geladen werden.",
                    exception
            );
        }
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}