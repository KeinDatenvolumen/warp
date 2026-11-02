import { postJson } from './restClient'
import type {
    RouteWeatherAiAnalysisRequest,
    RouteWeatherAiAnalysisResponse,
} from '../types/weatherAiAnalysis'

export async function fetchRouteWeatherAiAnalysis(
    request: RouteWeatherAiAnalysisRequest,
): Promise<RouteWeatherAiAnalysisResponse> {
    return postJson<
        RouteWeatherAiAnalysisRequest,
        RouteWeatherAiAnalysisResponse
    >('/api/v1/routes/weather-ai-analysis', request, {
        csrf: true,
    })
}
