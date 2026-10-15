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