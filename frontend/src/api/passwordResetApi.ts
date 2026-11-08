// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {postJson} from './restClient'
import type {
    PasswordResetConfirmRequest,
    PasswordResetSendRequest
} from '../types/passwordReset'

export function sendPasswordResetEmail(
    email: string
): Promise<void> {
    return postJson<PasswordResetSendRequest, void>(
        '/api/v1/password-resets/send',
        {email},
        {csrf: true}
    )
}

export function confirmPasswordReset(
    token: string,
    newPassword: string
): Promise<void> {
    return postJson<PasswordResetConfirmRequest, void>(
        '/api/v1/password-resets/confirm',
        {
            token,
            newPassword
        },
        {csrf: true}
    )
}