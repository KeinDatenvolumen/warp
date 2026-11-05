// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.infrastructure.openmeteo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherForecastBundle;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.infrastructure.openmeteo.exception.InvalidOpenMeteoResponseException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoRateLimitException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenMeteoProviderAndValidatorTest {

    private WireMockServer wireMockServer;
    private OpenMeteoWeatherProvider provider;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        OpenMeteoProperties properties = new OpenMeteoProperties(
                URI.create(wireMockServer.baseUrl()),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                50,
                168,
                List.of()
        );

        OpenMeteoConfiguration config = new OpenMeteoConfiguration();
        OpenMeteoClient client = new OpenMeteoClient(
                config.openMeteoRestClient(properties),
                new ObjectMapper()
        );
        OpenMeteoResponseValidator validator = new OpenMeteoResponseValidator();
        provider = new OpenMeteoWeatherProvider(client, validator, properties);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void parsesValidMultiLocationForecastUsingStandardBestMatchModel() throws Exception {
        String sampleJson;
        try (InputStream is = getClass().getResourceAsStream("/openmeteo/forecast-multiple-locations.json")) {
            sampleJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        wireMockServer.stubFor(
                get(urlPathEqualTo("/v1/forecast"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(sampleJson))
        );

        Instant start = Instant.ofEpochSecond(1791892800L);
        ForecastPeriod period = new ForecastPeriod(start, start.plusSeconds(3600));

        WeatherForecastBundle bundle = provider.getForecast(
                List.of(
                        new ForecastLocation("loc_1", 49.36, 9.16, null),
                        new ForecastLocation("loc_2", 49.40, 8.67, null)
                ),
                period
        );

        assertThat(bundle.locations()).hasSize(2);
        assertThat(bundle.locations().getFirst().bestMatch().model())
                .isEqualTo(WeatherModel.BEST_MATCH);
        assertThat(bundle.locations().getFirst().modelForecasts()).isEmpty();
        assertThat(bundle.locations().getFirst().bestMatch().hourly()).hasSize(2);
    }

    @Test
    void throwsInvalidOpenMeteoResponseExceptionOnIncompleteHourlyData() {
        String incompleteJson = """
                {
                  "latitude": 49.36,
                  "longitude": 9.16,
                  "elevation": 180.0,
                  "hourly": {
                    "time": [1791892800, 1791896400],
                    "temperature_2m": [12.4],
                    "wind_speed_10m": [10.0, 11.0],
                    "precipitation": [0.0, 0.1],
                    "weather_code": [1, 2]
                  }
                }
                """;

        wireMockServer.stubFor(
                get(urlPathEqualTo("/v1/forecast"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(incompleteJson))
        );

        Instant start = Instant.ofEpochSecond(1791892800L);
        ForecastPeriod period = new ForecastPeriod(start, start.plusSeconds(3600));

        assertThatThrownBy(() -> provider.getForecast(
                List.of(new ForecastLocation("loc_1", 49.36, 9.16, null)),
                period
        )).isInstanceOf(InvalidOpenMeteoResponseException.class)
                .hasMessageContaining("temperature_2m");
    }

    @Test
    void translatesHttp429And503IntoDedicatedExceptions() {
        Instant start = Instant.ofEpochSecond(1791892800L);
        ForecastPeriod period = new ForecastPeriod(start, start.plusSeconds(3600));
        List<ForecastLocation> locations = List.of(new ForecastLocation("loc_1", 49.36, 9.16, null));

        wireMockServer.stubFor(
                get(urlPathEqualTo("/v1/forecast"))
                        .willReturn(aResponse().withStatus(429))
        );

        assertThatThrownBy(() -> provider.getForecast(locations, period))
                .isInstanceOf(OpenMeteoRateLimitException.class);

        wireMockServer.stubFor(
                get(urlPathEqualTo("/v1/forecast"))
                        .willReturn(aResponse().withStatus(503))
        );

        assertThatThrownBy(() -> provider.getForecast(locations, period))
                .isInstanceOf(OpenMeteoUnavailableException.class);
    }
}
