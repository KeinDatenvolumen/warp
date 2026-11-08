// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {postJson} from './restClient'
import type {
    ChangeEmailRequest,
    ChangePasswordRequest
} from '../types/user'

export function changePassword(
    request: ChangePasswordRequest
): Promise<void> {
    return postJson<ChangePasswordRequest, void>(
        '/api/v1/users/me/password-changes',
        request,
        {csrf: true}
    )
}

export function changeEmail(
    request: ChangeEmailRequest
): Promise<void> {
    return postJson<ChangeEmailRequest, void>(
        '/api/v1/users/me/email-changes',
        request,
        {csrf: true}
    )
}