// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
import { postJson } from './restClient'
import type {
    WeatherOptimizationRequest,
    WeatherOptimizationResponse,
} from '../types/weatherOptimization'

export async function fetchWeatherOptimization(
    request: WeatherOptimizationRequest,
): Promise<WeatherOptimizationResponse> {
    return postJson<WeatherOptimizationRequest, WeatherOptimizationResponse>(
        '/api/v1/routes/weather-optimization',
        request,
        { csrf: true },
    )
}
