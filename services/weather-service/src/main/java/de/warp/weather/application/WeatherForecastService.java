// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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