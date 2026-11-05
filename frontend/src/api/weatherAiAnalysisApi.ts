// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
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
