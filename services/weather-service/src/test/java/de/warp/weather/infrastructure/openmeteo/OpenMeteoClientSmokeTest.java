// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.infrastructure.openmeteo;

import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Disabled("Calls the real Open-Meteo API manually")
class OpenMeteoClientSmokeTest {

    @Autowired
    private OpenMeteoClient client;

    @Test
    void loadsForecastsForMultipleLocations() {
        Instant start = Instant.now()
                .truncatedTo(ChronoUnit.HOURS)
                .plus(1, ChronoUnit.HOURS);

        ForecastPeriod period =
                new ForecastPeriod(
                        start,
                        start.plus(24, ChronoUnit.HOURS)
                );

        var responses = client.getForecast(
                List.of(
                        new ForecastLocation(
                                "mosbach",
                                49.3536,
                                9.1511,
                                null
                        ),
                        new ForecastLocation(
                                "heidelberg",
                                49.3988,
                                8.6724,
                                null
                        )
                ),
                period,
                WeatherModel.BEST_MATCH
        );

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().hourly()).isNotNull();
        assertThat(responses.getFirst().hourly().time())
                .isNotEmpty();
    }
}