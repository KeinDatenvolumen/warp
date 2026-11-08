// [KI-Log Nr. KI-167] (af1ca24) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: add frontend email verification (WARP-007)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export interface EmailVerificationConfirmRequest {
    token: string
}

export interface EmailVerificationResendRequest {
    email: string
}
