package de.warp.weather.api;

import de.warp.weather.api.dto.RouteWeatherAiAnalysisRequest;
import de.warp.weather.api.dto.RouteWeatherAiAnalysisResponse;
import de.warp.weather.api.dto.RouteWeatherOptimizationRequest;
import de.warp.weather.api.dto.RouteWeatherOptimizationResponse;
import de.warp.weather.application.RouteWeatherAiAnalysisService;
import de.warp.weather.application.RouteWeatherOptimizationService;
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
    private final RouteWeatherOptimizationService routeWeatherOptimizationService;
    private final RouteWeatherAiAnalysisService routeWeatherAiAnalysisService;

    @Autowired
    public InternalWeatherForecastController(
            WeatherForecastService weatherForecastService,
            @Autowired(required = false)
            WeatherEvaluationService weatherEvaluationService,
            @Autowired(required = false)
            RouteWeatherOptimizationService routeWeatherOptimizationService,
            @Autowired(required = false)
            RouteWeatherAiAnalysisService routeWeatherAiAnalysisService
    ) {
        this.weatherForecastService = weatherForecastService;
        this.weatherEvaluationService = weatherEvaluationService;
        this.routeWeatherOptimizationService = routeWeatherOptimizationService;
        this.routeWeatherAiAnalysisService = routeWeatherAiAnalysisService;
    }

    public InternalWeatherForecastController(
            WeatherForecastService weatherForecastService
    ) {
        this(weatherForecastService, null, null, null);
    }

    public InternalWeatherForecastController(
            WeatherForecastService weatherForecastService,
            WeatherEvaluationService weatherEvaluationService,
            RouteWeatherOptimizationService routeWeatherOptimizationService
    ) {
        this(
                weatherForecastService,
                weatherEvaluationService,
                routeWeatherOptimizationService,
                null
        );
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

    @PostMapping("/optimization")
    public RouteWeatherOptimizationResponse optimizeDeparture(
            @RequestBody RouteWeatherOptimizationRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        if (routeWeatherOptimizationService == null) {
            throw new IllegalStateException("Route weather optimization service is not configured");
        }
        return routeWeatherOptimizationService.optimizeDeparture(request);
    }

    @PostMapping("/ai-analysis")
    public RouteWeatherAiAnalysisResponse analyzeRouteWeatherAi(
            @RequestBody RouteWeatherAiAnalysisRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        if (routeWeatherAiAnalysisService == null) {
            throw new IllegalStateException("Route weather AI analysis service is not configured");
        }
        return routeWeatherAiAnalysisService.analyzeRouteWeather(request);
    }
}
