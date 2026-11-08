// [KI-Log Nr. KI-062] (1b34cbd) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add central activity profile configuration (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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

        assertThat(profile.weatherQueryIntervalKm())
                .isEqualTo(5);
    }

    @Test
    void providesWeatherQueryIntervalsInKilometers() {
        assertThat(
                service.getProfile(ActivityProfileCode.HIKING)
                        .weatherQueryIntervalKm()
        ).isEqualTo(2);

        assertThat(
                service.getProfile(ActivityProfileCode.BICYCLE)
                        .weatherQueryIntervalKm()
        ).isEqualTo(5);

        assertThat(
                service.getProfile(ActivityProfileCode.MOTORCYCLE)
                        .weatherQueryIntervalKm()
        ).isEqualTo(15);
    }
}