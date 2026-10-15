package de.warp.weather.api;

import de.warp.weather.application.WeatherForecastService;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherForecastBundle;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/internal/api/v1/weather")
public class InternalWeatherForecastController {

    private final WeatherForecastService weatherForecastService;

    public InternalWeatherForecastController(
            WeatherForecastService weatherForecastService
    ) {
        this.weatherForecastService = weatherForecastService;
    }

    @GetMapping("/forecast")
    public WeatherForecastBundle getForecast(
            @RequestParam String locationId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) Double elevationMeters,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end
    ) {
        ForecastLocation location = new ForecastLocation(
                locationId,
                latitude,
                longitude,
                elevationMeters
        );

        ForecastPeriod period = new ForecastPeriod(
                start,
                end
        );

        return weatherForecastService.getForecast(
                List.of(location),
                period
        );
    }
}