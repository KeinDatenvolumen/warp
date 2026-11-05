// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
export interface ApiFieldError {
    field?: string
    code?: string
    message?: string
}

export interface ApiErrorResponse {
    status?: number
    code?: string
    detail?: string
    message?: string
    fieldErrors?: ApiFieldError[]
}