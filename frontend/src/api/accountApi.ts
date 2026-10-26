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