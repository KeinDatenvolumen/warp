package de.warp.routing.profile.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.warp.routing.profile.domain.ActivityProfileCode;

class ActivityProfileServiceTests {

    private final ActivityProfileService service =
            new ActivityProfileService();

    @Test
    void providesAllSupportedActivityProfiles() {
        var profiles = service.getAllProfiles();

        assertThat(profiles)
                .extracting(profile -> profile.code())
                .containsExactly(
                        ActivityProfileCode.BICYCLE,
                        ActivityProfileCode.MOTORCYCLE,
                        ActivityProfileCode.HIKING
                );
    }

    @Test
    void providesCentralParametersForBicycle() {
        var profile = service.getProfile(
                ActivityProfileCode.BICYCLE
        );

        assertThat(profile.displayName())
                .isEqualTo("Fahrrad");

        assertThat(profile.averageSpeedKmh())
                .isEqualTo(18.0);

        assertThat(profile.weatherQueryIntervalMinutes())
                .isEqualTo(15);
    }
}