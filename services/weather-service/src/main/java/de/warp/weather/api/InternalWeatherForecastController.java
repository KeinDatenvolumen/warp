package de.warp.weather.api;

import de.warp.weather.application.WeatherEvaluationService;
import de.warp.weather.application.WeatherForecastService;
import de.warp.weather.domain.model.ForecastLocation;
import de.warp.weather.domain.model.ForecastPeriod;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherForecastBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping({"/internal/api/v1/weather", "/internal/v1/weather"})
public class InternalWeatherForecastController {

    private final WeatherForecastService weatherForecastService;
    private final WeatherEvaluationService weatherEvaluationService;

    @Autowired
    public InternalWeatherForecastController(
            WeatherForecastService weatherForecastService,
            @Autowired(required = false)
            WeatherEvaluationService weatherEvaluationService
    ) {
        this.weatherForecastService = weatherForecastService;
        this.weatherEvaluationService = weatherEvaluationService;
    }

    public InternalWeatherForecastController(
            WeatherForecastService weatherForecastService
    ) {
        this(weatherForecastService, null);
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

    @PostMapping("/evaluations")
    public WeatherEvaluationResponse evaluateRouteWeather(
            @RequestBody EvaluateRouteWeatherRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        WeatherEvaluation evaluation = weatherEvaluationService.evaluateRoute(
                request.requestedByUserId(),
                request.toDomain()
        );
        return WeatherEvaluationResponse.from(evaluation);
    }

    @GetMapping("/evaluations/{evaluationId}")
    public WeatherEvaluationResponse getEvaluation(
            @PathVariable String evaluationId
    ) {
        WeatherEvaluation evaluation =
                weatherEvaluationService.getEvaluationById(evaluationId);
        return WeatherEvaluationResponse.from(evaluation);
    }

    @GetMapping("/evaluations/by-route/{routeCalculationId}")
    public WeatherEvaluationResponse getEvaluationByRoute(
            @PathVariable String routeCalculationId
    ) {
        WeatherEvaluation evaluation =
                weatherEvaluationService.getEvaluationByRouteCalculationId(
                        routeCalculationId
                );
        return WeatherEvaluationResponse.from(evaluation);
    }
}
