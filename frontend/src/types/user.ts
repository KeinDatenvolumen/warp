// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
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