// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.weather;

import de.warp.bff.infrastructure.weather.WeatherApiDtos.EvaluateRouteWeatherRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationRequest;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationResponse;
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

    public RouteWeatherOptimizationResponse optimizeDeparture(
            RouteWeatherOptimizationRequest request
    ) {
        return restClient.post()
                .uri("/internal/api/v1/weather/optimization")
                .body(request)
                .retrieve()
                .body(RouteWeatherOptimizationResponse.class);
    }

    public RouteWeatherAiAnalysisResponse analyzeRouteWeatherAi(
            RouteWeatherAiAnalysisRequest request
    ) {
        return restClient.post()
                .uri("/internal/api/v1/weather/ai-analysis")
                .body(request)
                .retrieve()
                .body(RouteWeatherAiAnalysisResponse.class);
    }
}
