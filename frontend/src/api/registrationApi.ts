// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
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