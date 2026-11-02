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
