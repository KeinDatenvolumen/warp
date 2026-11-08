// [KI-Log Nr. KI-188] (c4f557c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-013): route calculation, departure time selection, and result display
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import type {
    ActivityProfile,
    ActivityProfileCode,
} from './activityProfile'

export type RouteCalculationStatus =
    | 'PENDING'
    | 'PROCESSING'
    | 'COMPLETED'
    | 'FAILED'

export interface RoutePointInput {
    label: string
    latitude: number
    longitude: number
}

export interface RouteCalculationInput {
    start: RoutePointInput
    destination: RoutePointInput
    departureTime: string
    activityProfile: ActivityProfileCode
}

export interface RouteCalculationMutationError {
    field: string | null
    code: string
    message: string
}

export interface RouteCalculationError {
    code: string
    message: string
}

export interface RouteGeometry {
    type: string
    coordinates: [number, number][]
}

export interface SegmentWeather {
    windSpeedKmh: number | null
    windDirectionDegrees: number | null
    windGustsKmh?: number | null
    precipitationProbabilityPercent: number | null
    precipitationMm: number | null
    wetRoadIndex?: number | null
    thunderstormProbabilityPercent: number | null
    temperatureCelsius?: number | null
    apparentTemperatureCelsius?: number | null
    weatherCode?: number | null
    limitingFactor?: string | null
    reasonLabel?: string | null
}

export interface RouteWeatherSegment {
    segmentIndex: number
    distanceFromStartMeters?: number | null
    expectedArrivalTime: string
    score: number
    color: string
    limitingFactor?: string | null
    reasonLabel?: string | null
    weather?: SegmentWeather | null
    geometry?: RouteGeometry | null
}

export interface RouteCalculation {
    id: string
    status: RouteCalculationStatus
    start: RoutePointInput
    destination: RoutePointInput
    activityProfile: ActivityProfile
    departureTime: string
    arrivalTime: string | null
    distanceMeters: number | null
    durationSeconds: number | null
    geometry: RouteGeometry | null
    weatherEvaluationId?: string | null
    weatherScore?: number | null
    weatherScoreVersion?: string | null
    thunderstormCapApplied?: boolean | null
    weatherSegments?: RouteWeatherSegment[] | null
    createdAt: string
    error: RouteCalculationError | null
}

export interface StartRouteCalculationMutationData {
    startRouteCalculation: {
        calculation: Pick<
            RouteCalculation,
            'id' | 'status'
        > | null
        errors: RouteCalculationMutationError[]
    }
}

export interface StartRouteCalculationMutationVariables {
    input: RouteCalculationInput
}

export interface RouteCalculationQueryData {
    routeCalculation: RouteCalculation | null
}

export interface RouteCalculationQueryVariables {
    id: string
}
