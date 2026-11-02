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
