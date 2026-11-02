package de.warp.weather.infrastructure.openmeteo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoForecastResponse;
import de.warp.weather.infrastructure.openmeteo.exception.InvalidOpenMeteoResponseException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoRateLimitException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoTimeoutException;
import de.warp.weather.infrastructure.openmeteo.exception.OpenMeteoUnavailableException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class OpenMeteoClient {

    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm")
                    .withZone(ZoneOffset.UTC);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoClient(
            RestClient openMeteoRestClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = openMeteoRestClient;
        this.objectMapper = objectMapper;
    }

    public List<OpenMeteoForecastResponse> getForecast(
            List<ForecastLocation> locations,
            ForecastPeriod period,
            WeatherModel model
    ) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/v1/forecast")
                                .queryParam(
                                        "latitude",
                                        joinLatitudes(locations)
                                )
                                .queryParam(
                                        "longitude",
                                        joinLongitudes(locations)
                                )
                                .queryParam(
                                        "hourly",
                                        OpenMeteoConstants.HOURLY_PARAMETER
                                )
                                .queryParam(
                                        "minutely_15",
                                        OpenMeteoConstants.MINUTELY_15_PARAMETER
                                )
                                .queryParam(
                                        "temperature_unit",
                                        "celsius"
                                )
                                .queryParam(
                                        "wind_speed_unit",
                                        "kmh"
                                )
                                .queryParam(
                                        "precipitation_unit",
                                        "mm"
                                )
                                .queryParam(
                                        "timeformat",
                                        "unixtime"
                                )
                                .queryParam(
                                        "timezone",
                                        "GMT"
                                )
                                .queryParam(
                                        "start_hour",
                                        formatHour(period.start())
                                )
                                .queryParam(
                                        "end_hour",
                                        formatHour(period.end())
                                )
                                .queryParam(
                                        "start_minutely_15",
                                        formatHour(period.start())
                                )
                                .queryParam(
                                        "end_minutely_15",
                                        formatHour(period.end())
                                )
                                .queryParam(
                                        "cell_selection",
                                        "land"
                                );

                        if (hasElevationForEveryLocation(locations)) {
                            builder.queryParam(
                                    "elevation",
                                    joinElevations(locations)
                            );
                        }

                        if (!model.isBestMatch()) {
                            builder.queryParam(
                                    "models",
                                    model.openMeteoValue()
                            );
                        }

                        return builder.build();
                    })
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 429,
                            (request, res) -> {
                                throw new OpenMeteoRateLimitException(
                                        "Open-Meteo rate limit reached"
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, res) -> {
                                throw new OpenMeteoUnavailableException(
                                        "Open-Meteo returned HTTP "
                                                + res.getStatusCode().value()
                                );
                            }
                    )
                    .body(JsonNode.class);

            return mapResponse(response);
        } catch (OpenMeteoRateLimitException
                 | OpenMeteoUnavailableException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new OpenMeteoTimeoutException(
                    "Open-Meteo request timed out or could not connect",
                    exception
            );
        } catch (Exception exception) {
            throw new InvalidOpenMeteoResponseException(
                    "Could not process Open-Meteo response",
                    exception
            );
        }
    }

    private List<OpenMeteoForecastResponse> mapResponse(
            JsonNode response
    ) {
        if (response == null || response.isNull()) {
            throw new InvalidOpenMeteoResponseException(
                    "Open-Meteo returned an empty response"
            );
        }

        if (response.isArray()) {
            List<OpenMeteoForecastResponse> result =
                    new ArrayList<>();

            for (JsonNode item : response) {
                result.add(
                        objectMapper.convertValue(
                                item,
                                OpenMeteoForecastResponse.class
                        )
                );
            }

            return List.copyOf(result);
        }

        if (response.isObject()) {
            return List.of(
                    objectMapper.convertValue(
                            response,
                            OpenMeteoForecastResponse.class
                    )
            );
        }

        throw new InvalidOpenMeteoResponseException(
                "Unexpected Open-Meteo JSON root type"
        );
    }

    private String joinLatitudes(
            List<ForecastLocation> locations
    ) {
        return locations.stream()
                .map(location -> format(location.latitude()))
                .collect(Collectors.joining(","));
    }

    private String joinLongitudes(
            List<ForecastLocation> locations
    ) {
        return locations.stream()
                .map(location -> format(location.longitude()))
                .collect(Collectors.joining(","));
    }

    private String joinElevations(
            List<ForecastLocation> locations
    ) {
        return locations.stream()
                .map(ForecastLocation::elevationMeters)
                .map(this::format)
                .collect(Collectors.joining(","));
    }

    private boolean hasElevationForEveryLocation(
            List<ForecastLocation> locations
    ) {
        return locations.stream()
                .allMatch(
                        location ->
                                location.elevationMeters() != null
                );
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private String formatHour(java.time.Instant instant) {
        return HOUR_FORMATTER.format(
                instant.truncatedTo(ChronoUnit.HOURS)
        );
    }
}
