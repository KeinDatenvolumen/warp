package de.warp.bff.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import de.warp.bff.api.dto.ActivityProfileResponse;

@Service
public class ActivityProfileService {

    private final RestClient restClient;
    private final String routingServiceUrl;

    public ActivityProfileService(
            RestClient restClient,
            @Value("${warp.services.routing.url}") String routingServiceUrl
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

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}