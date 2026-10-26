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