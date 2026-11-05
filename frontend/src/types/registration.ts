// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
export interface RegistrationRequest {
    name: string
    email: string
    password: string
}

export interface RegistrationResponse {
    id: string
    name: string
    email: string
    emailVerified: boolean
    createdAt: string
    message: string
}