package de.warp.weather.infrastructure.openmeteo;

import de.warp.weather.application.port.WeatherProvider;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherForecastBundle;
import de.warp.weather.domain.model.WeatherForecastLocation;
import de.warp.weather.domain.model.WeatherForecastSeries;
import de.warp.weather.domain.model.WeatherForecastTimeStep;
import de.warp.weather.domain.model.WeatherModel;
import de.warp.weather.domain.model.WeatherValues;
import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoForecastResponse;
import de.warp.weather.infrastructure.openmeteo.dto.OpenMeteoHourlyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class OpenMeteoWeatherProvider implements WeatherProvider {

    private final OpenMeteoClient client;
    private final OpenMeteoResponseValidator validator;
    private final OpenMeteoProperties properties;
    private final Clock clock;

    @Autowired
    public OpenMeteoWeatherProvider(
            OpenMeteoClient client,
            OpenMeteoResponseValidator validator,
            OpenMeteoProperties properties,
            Clock clock
    ) {
        this.client = Objects.requireNonNull(client);
        this.validator = Objects.requireNonNull(validator);
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
    }

    public OpenMeteoWeatherProvider(
            OpenMeteoClient client,
            OpenMeteoResponseValidator validator,
            OpenMeteoProperties properties
    ) {
        this(client, validator, properties, Clock.systemUTC());
    }

    @Override
    public WeatherForecastBundle getForecast(
            List<ForecastLocation> locations,
            ForecastPeriod period
    ) {
        validateRequest(locations, period);

        Map<String, WeatherForecastSeries> bestMatch =
                loadForecasts(locations, period, WeatherModel.BEST_MATCH);

        List<WeatherForecastLocation> result =
                new ArrayList<>(locations.size());

        for (ForecastLocation location : locations) {
            result.add(new WeatherForecastLocation(
                    location,
                    bestMatch.get(location.id()),
                    Map.of()
            ));
        }

        return new WeatherForecastBundle(
                Instant.now(clock),
                result
        );
    }

    private Map<String, WeatherForecastSeries> loadForecasts(
            List<ForecastLocation> locations,
            ForecastPeriod period,
            WeatherModel model
    ) {
        List<OpenMeteoForecastResponse> responses =
                client.getForecast(locations, period, model);

        validator.validateBatch(responses, locations.size());

        Map<String, WeatherForecastSeries> forecasts =
                new LinkedHashMap<>();

        for (int index = 0; index < locations.size(); index++) {
            ForecastLocation location = locations.get(index);
            OpenMeteoForecastResponse response = responses.get(index);

            forecasts.put(
                    location.id(),
                    mapSeries(response, model)
            );
        }

        return forecasts;
    }

    private WeatherForecastSeries mapSeries(
            OpenMeteoForecastResponse response,
            WeatherModel model
    ) {
        OpenMeteoHourlyResponse hourly = response.hourly();
        List<WeatherForecastTimeStep> timeSteps =
                new ArrayList<>(hourly.time().size());

        for (int index = 0; index < hourly.time().size(); index++) {
            timeSteps.add(new WeatherForecastTimeStep(
                    Instant.ofEpochSecond(hourly.time().get(index)),
                    mapValues(hourly, index)
            ));
        }

        return new WeatherForecastSeries(
                model,
                response.latitude(),
                response.longitude(),
                response.elevation(),
                timeSteps
        );
    }

    private WeatherValues mapValues(
            OpenMeteoHourlyResponse hourly,
            int index
    ) {
        Double isDay = valueAt(hourly.isDay(), index);
        Double weatherCode = valueAt(hourly.weatherCode(), index);

        return new WeatherValues(
                valueAt(hourly.temperature2m(), index),
                valueAt(hourly.apparentTemperature(), index),
                valueAt(hourly.relativeHumidity2m(), index),
                valueAt(hourly.dewPoint2m(), index),
                valueAt(hourly.wetBulbTemperature2m(), index),
                valueAt(hourly.precipitationProbability(), index),
                valueAt(hourly.precipitation(), index),
                valueAt(hourly.rain(), index),
                valueAt(hourly.showers(), index),
                valueAt(hourly.snowfall(), index),
                valueAt(hourly.snowDepth(), index),
                valueAt(hourly.windSpeed10m(), index),
                valueAt(hourly.windDirection10m(), index),
                valueAt(hourly.windGusts10m(), index),
                valueAt(hourly.pressureMsl(), index),
                valueAt(hourly.surfacePressure(), index),
                valueAt(hourly.cloudCover(), index),
                valueAt(hourly.cloudCoverLow(), index),
                valueAt(hourly.cloudCoverMid(), index),
                valueAt(hourly.cloudCoverHigh(), index),
                valueAt(hourly.visibility(), index),
                valueAt(hourly.uvIndex(), index),
                valueAt(hourly.uvIndexClearSky(), index),
                valueAt(hourly.shortwaveRadiation(), index),
                valueAt(hourly.directRadiation(), index),
                valueAt(hourly.diffuseRadiation(), index),
                valueAt(hourly.sunshineDuration(), index),
                isDay == null ? null : isDay >= 0.5,
                valueAt(hourly.cape(), index),
                valueAt(hourly.liftedIndex(), index),
                valueAt(hourly.convectiveInhibition(), index),
                valueAt(hourly.freezingLevelHeight(), index),
                valueAt(hourly.vapourPressureDeficit(), index),
                valueAt(hourly.evapotranspiration(), index),
                valueAt(hourly.soilTemperature0cm(), index),
                weatherCode == null ? null : weatherCode.intValue()
        );
    }

    private <T> T valueAt(List<T> values, int index) {
        if (values == null || index >= values.size()) {
            return null;
        }

        return values.get(index);
    }

    private void validateRequest(
            List<ForecastLocation> locations,
            ForecastPeriod period
    ) {
        Objects.requireNonNull(locations, "locations must not be null");
        Objects.requireNonNull(period, "period must not be null");

        if (locations.isEmpty()) {
            throw new IllegalArgumentException(
                    "locations must not be empty"
            );
        }

        if (locations.size() > properties.maximumLocationsPerRequest()) {
            throw new IllegalArgumentException(
                    "locations must not contain more than "
                            + properties.maximumLocationsPerRequest()
                            + " entries"
            );
        }

        Map<String, Boolean> locationIds = new LinkedHashMap<>();

        for (ForecastLocation location : locations) {
            Objects.requireNonNull(
                    location,
                    "locations must not contain null entries"
            );

            if (locationIds.put(location.id(), Boolean.TRUE) != null) {
                throw new IllegalArgumentException(
                        "location ids must be unique: " + location.id()
                );
            }
        }
    }
}
