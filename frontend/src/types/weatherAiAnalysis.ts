// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Change Request Ereigniskarte 12 (MCP & Gemini)
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
