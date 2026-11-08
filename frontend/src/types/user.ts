// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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