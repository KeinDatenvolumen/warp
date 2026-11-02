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
        List<WeatherForecastTimeStep> hourlySteps = mapTimeSteps(hourly);

        OpenMeteoHourlyResponse minutely15 = response.minutely15();
        List<WeatherForecastTimeStep> minutely15Steps = minutely15 != null && minutely15.time() != null
                ? mapTimeSteps(minutely15)
                : List.of();

        return new WeatherForecastSeries(
                model,
                response.latitude(),
                response.longitude(),
                response.elevation(),
                hourlySteps,
                minutely15Steps
        );
    }

    private List<WeatherForecastTimeStep> mapTimeSteps(
            OpenMeteoHourlyResponse series
    ) {
        if (series == null || series.time() == null || series.time().isEmpty()) {
            return List.of();
        }

        List<WeatherForecastTimeStep> timeSteps =
                new ArrayList<>(series.time().size());

        for (int index = 0; index < series.time().size(); index++) {
            Long epochSecond = series.time().get(index);
            if (epochSecond != null) {
                timeSteps.add(new WeatherForecastTimeStep(
                        Instant.ofEpochSecond(epochSecond),
                        mapValues(series, index)
                ));
            }
        }

        return List.copyOf(timeSteps);
    }

    private WeatherValues mapValues(
            OpenMeteoHourlyResponse series,
            int index
    ) {
        Double isDay = valueAt(series.isDay(), index);
        Double weatherCode = valueAt(series.weatherCode(), index);

        Double shortwave = valueAt(series.shortwaveRadiationInstant(), index);
        if (shortwave == null) {
            shortwave = valueAt(series.shortwaveRadiation(), index);
        }

        Double soilTemp = valueAt(series.soilTemperature0To7cm(), index);
        if (soilTemp == null) {
            soilTemp = valueAt(series.soilTemperature0cm(), index);
        }

        return new WeatherValues(
                valueAt(series.temperature2m(), index),
                valueAt(series.apparentTemperature(), index),
                valueAt(series.relativeHumidity2m(), index),
                valueAt(series.dewPoint2m(), index),
                valueAt(series.wetBulbTemperature2m(), index),
                valueAt(series.precipitationProbability(), index),
                valueAt(series.precipitation(), index),
                valueAt(series.rain(), index),
                valueAt(series.showers(), index),
                valueAt(series.snowfall(), index),
                valueAt(series.snowDepth(), index),
                valueAt(series.windSpeed10m(), index),
                valueAt(series.windDirection10m(), index),
                valueAt(series.windGusts10m(), index),
                valueAt(series.pressureMsl(), index),
                valueAt(series.surfacePressure(), index),
                valueAt(series.cloudCover(), index),
                valueAt(series.cloudCoverLow(), index),
                valueAt(series.cloudCoverMid(), index),
                valueAt(series.cloudCoverHigh(), index),
                valueAt(series.visibility(), index),
                valueAt(series.uvIndex(), index),
                valueAt(series.uvIndexClearSky(), index),
                shortwave,
                valueAt(series.directRadiation(), index),
                valueAt(series.diffuseRadiation(), index),
                valueAt(series.sunshineDuration(), index),
                isDay == null ? null : isDay >= 0.5,
                valueAt(series.cape(), index),
                valueAt(series.liftedIndex(), index),
                valueAt(series.convectiveInhibition(), index),
                valueAt(series.freezingLevelHeight(), index),
                valueAt(series.vapourPressureDeficit(), index),
                valueAt(series.evapotranspiration(), index),
                soilTemp,
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
