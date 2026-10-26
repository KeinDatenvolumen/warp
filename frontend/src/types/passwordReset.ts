export interface PasswordResetSendRequest {
    email: string
}

export interface PasswordResetConfirmRequest {
    token: string
    newPassword: string
}