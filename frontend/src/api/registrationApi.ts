// [KI-Log Nr. KI-085] (d3fd446) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration API client (WARP-005)
// [KI-Log Nr. KI-108] (da79e47) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration API client (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {postJson} from './restClient'
import type {
    RegistrationRequest,
    RegistrationResponse
} from '../types/registration'

export function registerUser(
    request: RegistrationRequest
): Promise<RegistrationResponse> {
    return postJson<RegistrationRequest, RegistrationResponse>(
        '/api/v1/users',
        request
    )
}