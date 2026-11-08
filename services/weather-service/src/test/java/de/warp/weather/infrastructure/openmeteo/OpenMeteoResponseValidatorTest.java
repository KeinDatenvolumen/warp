// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.infrastructure.openmeteo;

import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoForecastResponse;
import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoHourlyResponse;
import de.warp.weather.infrastructure.openmeteo.exception.InvalidOpenMeteoResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenMeteoResponseValidator Unit Tests")
class OpenMeteoResponseValidatorTest {

    private OpenMeteoResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OpenMeteoResponseValidator();
    }

    private OpenMeteoHourlyResponse createHourly(
            List<Long> time,
            List<Double> temp,
            List<Double> windSpeed,
            List<Double> precip,
            List<Double> weatherCode,
            List<Double> windDir,
            List<Double> precipProb,
            List<Double> cape
    ) {
        return new OpenMeteoHourlyResponse(
                time, temp, null, null, null, null, precipProb,
                precip, null, null, null, null, windSpeed, windDir,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, cape, null,
                null, null, null, null, null, null, weatherCode
        );
    }

    private OpenMeteoForecastResponse createValidForecast(OpenMeteoHourlyResponse hourly) {
        return new OpenMeteoForecastResponse(
                49.35, 9.15, 200.0, 1.5, 0, "UTC", "UTC", hourly
        );
    }

    @Nested
    @DisplayName("validateBatch structure tests")
    class ValidateBatchTests {

        @Test
        @DisplayName("Rejects null batch response")
        void rejectsNullBatch() {
            assertThatThrownBy(() -> validator.validateBatch(null, 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("Open-Meteo response list is null");
        }

        @Test
        @DisplayName("Rejects count mismatch")
        void rejectsCountMismatch() {
            assertThatThrownBy(() -> validator.validateBatch(List.of(), 2))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("Open-Meteo returned 0 locations, expected 2");
        }
    }

    @Nested
    @DisplayName("Location and Hourly root validation tests")
    class LocationRootValidationTests {

        @Test
        @DisplayName("Rejects null response element in list")
        void rejectsNullResponse() {
            List<OpenMeteoForecastResponse> list = Arrays.asList(new OpenMeteoForecastResponse[]{null});
            assertThatThrownBy(() -> validator.validateBatch(list, 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("response object is null");
        }

        @Test
        @DisplayName("Rejects missing coordinates")
        void rejectsMissingCoordinates() {
            OpenMeteoForecastResponse noLat = new OpenMeteoForecastResponse(
                    null, 9.15, 200.0, 1.0, 0, "UTC", "UTC", null
            );
            assertThatThrownBy(() -> validator.validateBatch(List.of(noLat), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("resolved coordinates are missing");

            OpenMeteoForecastResponse noLon = new OpenMeteoForecastResponse(
                    49.35, null, 200.0, 1.0, 0, "UTC", "UTC", null
            );
            assertThatThrownBy(() -> validator.validateBatch(List.of(noLon), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("resolved coordinates are missing");
        }

        @Test
        @DisplayName("Rejects missing elevation")
        void rejectsMissingElevation() {
            OpenMeteoForecastResponse noElevation = new OpenMeteoForecastResponse(
                    49.35, 9.15, null, 1.0, 0, "UTC", "UTC", null
            );
            assertThatThrownBy(() -> validator.validateBatch(List.of(noElevation), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("resolved elevation is missing");
        }

        @Test
        @DisplayName("Rejects missing or empty hourly data")
        void rejectsMissingOrEmptyHourly() {
            OpenMeteoForecastResponse noHourly = createValidForecast(null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(noHourly), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("hourly data is missing");

            OpenMeteoHourlyResponse emptyTime = createHourly(List.of(), null, null, null, null, null, null, null);
            OpenMeteoForecastResponse emptyHourly = createValidForecast(emptyTime);
            assertThatThrownBy(() -> validator.validateBatch(List.of(emptyHourly), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("hourly time axis is missing or empty");

            OpenMeteoHourlyResponse nullTime = createHourly(null, null, null, null, null, null, null, null);
            OpenMeteoForecastResponse nullTimeHourly = createValidForecast(nullTime);
            assertThatThrownBy(() -> validator.validateBatch(List.of(nullTimeHourly), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("hourly time axis is missing or empty");

            List<Long> timeWithNull = Arrays.asList(100L, null);
            OpenMeteoHourlyResponse nullItemTime = createHourly(timeWithNull, null, null, null, null, null, null, null);
            OpenMeteoForecastResponse nullItemTimeHourly = createValidForecast(nullItemTime);
            assertThatThrownBy(() -> validator.validateBatch(List.of(nullItemTimeHourly), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("hourly time axis contains null values");
        }
    }

    @Nested
    @DisplayName("Series validation tests")
    class SeriesValidationTests {

        private final List<Long> validTime = List.of(100L, 200L);

        @Test
        @DisplayName("Rejects missing, unequal size, or null-containing required series")
        void validatesRequiredSeries() {
            // temperature_2m missing
            OpenMeteoHourlyResponse noTemp = createHourly(validTime, null, List.of(10.0, 10.0), List.of(0.0, 0.0), List.of(1.0, 1.0), null, null, null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(noTemp)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("temperature_2m is missing");

            // temperature_2m size mismatch
            OpenMeteoHourlyResponse wrongSizeTemp = createHourly(validTime, List.of(20.0), List.of(10.0, 10.0), List.of(0.0, 0.0), List.of(1.0, 1.0), null, null, null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(wrongSizeTemp)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("temperature_2m contains 1 entries, expected 2");

            // temperature_2m contains null
            List<Double> tempWithNull = Arrays.asList(20.0, null);
            OpenMeteoHourlyResponse nullTemp = createHourly(validTime, tempWithNull, List.of(10.0, 10.0), List.of(0.0, 0.0), List.of(1.0, 1.0), null, null, null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(nullTemp)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("temperature_2m contains null values");

            // wind_speed_10m missing
            OpenMeteoHourlyResponse noWind = createHourly(validTime, List.of(20.0, 21.0), null, List.of(0.0, 0.0), List.of(1.0, 1.0), null, null, null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(noWind)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("wind_speed_10m is missing");

            // precipitation missing
            OpenMeteoHourlyResponse noPrecip = createHourly(validTime, List.of(20.0, 21.0), List.of(10.0, 10.0), null, List.of(1.0, 1.0), null, null, null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(noPrecip)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("precipitation is missing");

            // weather_code missing
            OpenMeteoHourlyResponse noWeatherCode = createHourly(validTime, List.of(20.0, 21.0), List.of(10.0, 10.0), List.of(0.0, 0.0), null, null, null, null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(noWeatherCode)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("weather_code is missing");
        }

        @Test
        @DisplayName("Validates optional series lengths when present")
        void validatesOptionalSeries() {
            // wind_direction_10m length mismatch
            OpenMeteoHourlyResponse wrongWindDir = createHourly(validTime, List.of(20.0, 21.0), List.of(10.0, 10.0), List.of(0.0, 0.0), List.of(1.0, 1.0),
                    List.of(180.0), null, null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(wrongWindDir)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("wind_direction_10m contains 1 entries, expected 2");

            // precipitation_probability length mismatch
            OpenMeteoHourlyResponse wrongProb = createHourly(validTime, List.of(20.0, 21.0), List.of(10.0, 10.0), List.of(0.0, 0.0), List.of(1.0, 1.0),
                    null, List.of(10.0), null);
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(wrongProb)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("precipitation_probability contains 1 entries, expected 2");

            // cape length mismatch
            OpenMeteoHourlyResponse wrongCape = createHourly(validTime, List.of(20.0, 21.0), List.of(10.0, 10.0), List.of(0.0, 0.0), List.of(1.0, 1.0),
                    null, null, List.of(500.0));
            assertThatThrownBy(() -> validator.validateBatch(List.of(createValidForecast(wrongCape)), 1))
                    .isInstanceOf(InvalidOpenMeteoResponseException.class)
                    .hasMessageContaining("cape contains 1 entries, expected 2");
        }

        @Test
        @DisplayName("Passes validation when all required and optional series are valid")
        void passesCompleteValidResponse() {
            OpenMeteoHourlyResponse validHourly = createHourly(
                    validTime,
                    List.of(20.0, 21.0),
                    List.of(10.0, 12.0),
                    List.of(0.0, 0.5),
                    List.of(1.0, 2.0),
                    List.of(180.0, 190.0),
                    List.of(10.0, 20.0),
                    List.of(100.0, 200.0)
            );
            OpenMeteoForecastResponse validForecast = createValidForecast(validHourly);

            assertThatCode(() -> validator.validateBatch(List.of(validForecast), 1))
                    .doesNotThrowAnyException();
        }
    }
}
