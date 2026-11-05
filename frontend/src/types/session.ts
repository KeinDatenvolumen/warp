// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
export interface LoginRequest {
    email: string
    password: string
}

export interface CreateSessionResponse {
    userId: string
    expiresAt: string
}

export interface CurrentSession {
    userId: string
    createdAt: string
    expiresAt: string
}