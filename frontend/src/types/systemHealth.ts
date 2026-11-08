// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export interface ServiceHealth {
    service: string
    status: string
    reachable: boolean
    responseTimeMs: number | null
}

export interface SystemHealth {
    status: string
    checkedAt: string | null
    healthyServices: number
    totalServices: number
    services: ServiceHealth[]
}

export interface SystemHealthQueryData {
    systemHealth: SystemHealth | null
}