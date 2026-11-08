// [KI-Log Nr. KI-167] (af1ca24) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: add frontend email verification (WARP-007)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {postJson} from './restClient'
import type {
    EmailVerificationConfirmRequest,
    EmailVerificationResendRequest
} from '../types/emailVerification'

export function confirmEmail(token: string): Promise<void> {
    return postJson<EmailVerificationConfirmRequest, void>(
        '/api/v1/email-verifications/confirm',
        {token},
        {csrf: true}
    )
}

export function resendVerificationEmail(email: string): Promise<void> {
    return postJson<EmailVerificationResendRequest, void>(
        '/api/v1/email-verifications/resend',
        {email},
        {csrf: true}
    )
}
