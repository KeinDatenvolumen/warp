package de.warp.bff.infrastructure.weather;

import de.warp.bff.infrastructure.weather.WeatherApiDtos.EvaluateRouteWeatherRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.WeatherEvaluationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WeatherClient {

    private final RestClient restClient;

    public WeatherClient(
            @Qualifier("weatherServiceRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public WeatherEvaluationResponse evaluateRouteWeather(
            EvaluateRouteWeatherRequest request
    ) {
        return restClient.post()
                .uri("/internal/api/v1/weather/evaluations")
                .body(request)
                .retrieve()
                .body(WeatherEvaluationResponse.class);
    }

    public WeatherEvaluationResponse getEvaluationById(
            String evaluationId
    ) {
        return restClient.get()
                .uri(
                        "/internal/api/v1/weather/evaluations/{evaluationId}",
                        evaluationId
                )
                .retrieve()
                .body(WeatherEvaluationResponse.class);
    }

    public WeatherEvaluationResponse getEvaluationByRoute(
            String routeCalculationId
    ) {
        return restClient.get()
                .uri(
                        "/internal/api/v1/weather/evaluations/by-route/{routeCalculationId}",
                        routeCalculationId
                )
                .retrieve()
                .body(WeatherEvaluationResponse.class);
    }
}
