// [KI-Log Nr. KI-085] (d3fd446) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration API client (WARP-005)
// [KI-Log Nr. KI-108] (da79e47) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration API client (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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