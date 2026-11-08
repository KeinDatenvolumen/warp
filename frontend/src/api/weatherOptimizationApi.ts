// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
