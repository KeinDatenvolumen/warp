package de.warp.weather.application.port;

import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherForecastBundle;

import java.util.List;

public interface WeatherProvider {

    WeatherForecastBundle getForecast(
            List<ForecastLocation> locations,
            ForecastPeriod period
    );
}