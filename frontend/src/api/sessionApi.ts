// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
import {apolloClient} from './apolloClient'
import {CURRENT_SESSION_QUERY} from './currentSessionQuery'
import {containsSessionInvalidCode} from './graphqlErrors'
import {deleteRequest, postJson} from './restClient'
import type {
    CreateSessionResponse,
    CurrentSession,
    LoginRequest
} from '../types/session'

export function createSession(
    request: LoginRequest
): Promise<CreateSessionResponse> {
    return postJson<LoginRequest, CreateSessionResponse>(
        '/api/v1/sessions',
        request
    )
}

export function deleteCurrentSession(): Promise<void> {
    return deleteRequest('/api/v1/sessions/current')
}

export async function fetchCurrentSession(): Promise<CurrentSession | null> {
    const result = await apolloClient.query({
        query: CURRENT_SESSION_QUERY,
        fetchPolicy: 'no-cache',
        errorPolicy: 'all',
        context: {
            skipSessionInvalidNotification: true
        }
    })

    if (containsSessionInvalidCode(result.error)) {
        return null
    }

    if (result.error) {
        throw result.error
    }

    return result.data?.currentSession ?? null
}