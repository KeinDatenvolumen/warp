// [KI-Log Nr. KI-085] (d3fd446) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration API client (WARP-005)
// [KI-Log Nr. KI-091] (fc545e3) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: complete success response and automated coverage (WARP-005)
// [KI-Log Nr. KI-094] (a3d7118) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: remove message field from registration response (WARP-005)
// [KI-Log Nr. KI-108] (da79e47) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration API client (WARP-005)
// [KI-Log Nr. KI-112] (88962a1) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: complete success response and automated coverage (WARP-005)
// [KI-Log Nr. KI-115] (434109e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: remove message field from registration response (WARP-005)
// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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