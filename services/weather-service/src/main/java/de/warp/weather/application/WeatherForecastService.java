package de.warp.weather.application;

import de.warp.weather.application.port.WeatherProvider;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherForecastBundle;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WeatherForecastService {

    private final WeatherProvider weatherProvider;

    public WeatherForecastService(
            WeatherProvider weatherProvider
    ) {
        this.weatherProvider = weatherProvider;
    }

    public WeatherForecastBundle getForecast(
            List<ForecastLocation> locations,
            ForecastPeriod period
    ) {
        return weatherProvider.getForecast(
                locations,
                period
        );
    }
}