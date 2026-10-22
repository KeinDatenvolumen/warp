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
