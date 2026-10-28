export interface CurrentUser {
    id: string
    name: string
    email: string
    emailVerified: boolean
    createdAt: string
}

export interface ChangePasswordRequest {
    currentPassword: string
    newPassword: string
}

export interface ChangeEmailRequest {
    newEmail: string
    password: string
}