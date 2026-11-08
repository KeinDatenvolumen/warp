// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export type HiddenParameterStatus =
    | 'OPTIMAL'
    | 'NOTICE'
    | 'WARNING'
    | 'CRITICAL'

export interface HiddenParameterInsight {
    parameterKey: string
    parameterLabel: string
    category: string
    observedValue: string
    status: HiddenParameterStatus
    interpretation: string
}

export type CriticalSectionSeverity =
    | 'INFO'
    | 'WARNING'
    | 'DANGER'

export interface CriticalSectionHazard {
    segmentIndexStart: number
    segmentIndexEnd: number
    distanceRangeKm: string
    timeWindow: string
    minSegmentScore: number
    severity: CriticalSectionSeverity
    primaryFactor: string
    title: string
    description: string
    recommendation: string
}

export type AiRiskLevel =
    | 'LOW'
    | 'MODERATE'
    | 'HIGH'
    | 'CRITICAL'

export interface RouteWeatherAiAnalysisResponse {
    routeCalculationId: string
    activityProfileCode: string
    activityProfileDisplayName: string
    departureTime: string
    overallScore: number
    thunderstormCapApplied: boolean
    riskLevel: AiRiskLevel
    headline: string
    overallSummary: string
    activityProfileAdvice: string
    hiddenParameterInsights: HiddenParameterInsight[]
    criticalSections: CriticalSectionHazard[]
    recommendations: string[]
    mcpToolsUsed: string[]
    modelName: string
    generatedAt: string
}

export interface RouteWeatherAiAnalysisRequest {
    routeCalculationId?: string
    startLabel?: string
    destinationLabel?: string
    activityProfile?: string
    departureTime?: string
    durationSeconds?: number
    distanceMeters?: number
    coordinates?: [number, number][]
}
