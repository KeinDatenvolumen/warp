package de.warp.weather.infrastructure.openmeteo;

import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoForecastResponse;
import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoHourlyResponse;
import de.warp.weather.infrastructure.openmeteo.exception.InvalidOpenMeteoResponseException;
import org.springframework.stereotype.Component;

import java.util.List;

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

        for (int index = 0;
             index < responses.size();
             index++) {
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

        OpenMeteoHourlyResponse hourly =
                response.hourly();

        if (hourly == null) {
            throw invalid(
                    locationIndex,
                    "hourly data is missing"
            );
        }

        if (hourly.time() == null
                || hourly.time().isEmpty()) {
            throw invalid(
                    locationIndex,
                    "hourly time axis is missing or empty"
            );
        }

        validateRequiredLength(
                hourly.temperature2m(),
                hourly.time().size(),
                locationIndex,
                "temperature_2m"
        );

        validateRequiredLength(
                hourly.windSpeed10m(),
                hourly.time().size(),
                locationIndex,
                "wind_speed_10m"
        );

        validateRequiredLength(
                hourly.precipitation(),
                hourly.time().size(),
                locationIndex,
                "precipitation"
        );

        validateRequiredLength(
                hourly.weatherCode(),
                hourly.time().size(),
                locationIndex,
                "weather_code"
        );
    }

    private void validateRequiredLength(
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