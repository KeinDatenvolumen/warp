// [KI-Log Nr. KI-120] (61da337) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add session API with CSRF handling and Retry-After support (WARP-006)
// [KI-Log Nr. KI-234] (d6e6dbb) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): fix(frontend): prevent sessionInvalidLink and clearStore from cancelling startup queries when unauthenticated (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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