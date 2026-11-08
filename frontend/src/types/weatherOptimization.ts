// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export interface DepartureSlotResult {
    departureIso: string
    departureTime: string
    arrivalIso: string
    arrivalTime: string
    routeScore: number
    robustScore: number
    hasThunderCap: boolean
    reachesNightfall: boolean
    segmentScores: number[]
    segmentReasons: string[]
    summaryDeltaText: string
}

export interface OptimalWindowSummary {
    bestDeparture: string
    bestDepartureIso: string
    bestScore: number
    stableWindow: string
    advantageVsWorst: string
    worstDeparture: string
    worstScore: number
}

export interface WeatherOptimizationResponse {
    recommendedSlotIndex: number
    optimalWindowSummary: OptimalWindowSummary
    departureSlots: DepartureSlotResult[]
}

export interface WeatherOptimizationRequest {
    routeCalculationId?: string
    activityProfile?: string
    departureTime?: string
    durationSeconds?: number
    distanceMeters?: number
    coordinates?: [number, number][]
    windowStart?: string
    windowEnd?: string
}
