package de.warp.weather.infrastructure.openmeteo;

import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoForecastResponse;
import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoHourlyResponse;
import de.warp.weather.infrastructure.openmeteo.exception.InvalidOpenMeteoResponseException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OpenMeteoResponseValidator {

    public void validateBatch(
            List<OpenMeteoForecastResponse> responses,
            int expectedLocationCount
    ) {
        if (responses == null) {
            throw new InvalidOpenMeteoResponseException(
                    "Open-Meteo response list is null"
            );
        }

        if (responses.size() != expectedLocationCount) {
            throw new InvalidOpenMeteoResponseException(
                    "Open-Meteo returned "
                            + responses.size()
                            + " locations, expected "
                            + expectedLocationCount
            );
        }

        for (int index = 0; index < responses.size(); index++) {
            validateLocation(responses.get(index), index);
        }
    }

    private void validateLocation(
            OpenMeteoForecastResponse response,
            int locationIndex
    ) {
        if (response == null) {
            throw invalid(
                    locationIndex,
                    "response object is null"
            );
        }

        if (response.latitude() == null
                || response.longitude() == null) {
            throw invalid(
                    locationIndex,
                    "resolved coordinates are missing"
            );
        }

        if (response.elevation() == null) {
            throw invalid(
                    locationIndex,
                    "resolved elevation is missing"
            );
        }

        OpenMeteoHourlyResponse hourly = response.hourly();

        if (hourly == null) {
            throw invalid(
                    locationIndex,
                    "hourly data is missing"
            );
        }

        if (hourly.time() == null || hourly.time().isEmpty()) {
            throw invalid(
                    locationIndex,
                    "hourly time axis is missing or empty"
            );
        }

        if (hourly.time().stream().anyMatch(Objects::isNull)) {
            throw invalid(
                    locationIndex,
                    "hourly time axis contains null values"
            );
        }

        int expectedLength = hourly.time().size();

        validateRequiredSeries(
                hourly.temperature2m(),
                expectedLength,
                locationIndex,
                "temperature_2m"
        );

        validateRequiredSeries(
                hourly.windSpeed10m(),
                expectedLength,
                locationIndex,
                "wind_speed_10m"
        );

        validateRequiredSeries(
                hourly.precipitation(),
                expectedLength,
                locationIndex,
                "precipitation"
        );

        validateRequiredSeries(
                hourly.weatherCode(),
                expectedLength,
                locationIndex,
                "weather_code"
        );

        validateOptionalSeriesLength(
                hourly.windDirection10m(),
                expectedLength,
                locationIndex,
                "wind_direction_10m"
        );

        validateOptionalSeriesLength(
                hourly.precipitationProbability(),
                expectedLength,
                locationIndex,
                "precipitation_probability"
        );

        validateOptionalSeriesLength(
                hourly.cape(),
                expectedLength,
                locationIndex,
                "cape"
        );
    }

    private void validateRequiredSeries(
            List<?> values,
            int expectedLength,
            int locationIndex,
            String variable
    ) {
        if (values == null) {
            throw invalid(
                    locationIndex,
                    variable + " is missing"
            );
        }

        if (values.size() != expectedLength) {
            throw invalid(
                    locationIndex,
                    variable
                            + " contains "
                            + values.size()
                            + " entries, expected "
                            + expectedLength
            );
        }

        if (values.stream().anyMatch(Objects::isNull)) {
            throw invalid(
                    locationIndex,
                    variable + " contains null values"
            );
        }
    }

    private void validateOptionalSeriesLength(
            List<?> values,
            int expectedLength,
            int locationIndex,
            String variable
    ) {
        if (values != null && values.size() != expectedLength) {
            throw invalid(
                    locationIndex,
                    variable
                            + " contains "
                            + values.size()
                            + " entries, expected "
                            + expectedLength
            );
        }
    }

    private InvalidOpenMeteoResponseException invalid(
            int locationIndex,
            String detail
    ) {
        return new InvalidOpenMeteoResponseException(
                "Invalid Open-Meteo response for location index "
                        + locationIndex
                        + ": "
                        + detail
        );
    }
}
