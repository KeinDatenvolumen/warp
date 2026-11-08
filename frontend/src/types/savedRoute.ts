// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-198] (884c88d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add favorite API operations and toggle button (WARP-022)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-242] (a913a41) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: optimiere Routenplaner, Abfahrtszeit-Slider und gespeicherte Routen (WARP-014)
import type {ActivityProfile} from './activityProfile'

export interface RoutePlace {
    label: string
    latitude: number
    longitude: number
}

export interface RouteGeometry {
    type: string
    /** GeoJSON-Positionen: [Längengrad, Breitengrad] */
    coordinates: number[][]
}

export interface SavedRouteWeatherSegment {
    segmentIndex: number
    score: number
    color: string
    geometry: RouteGeometry | null
}

export interface SavedRouteSummary {
    id: string
    name: string
    start: RoutePlace
    destination: RoutePlace
    activityProfile: ActivityProfile
    weatherScore: number
    favorite: boolean
    createdAt: string
}

export interface PageInfo {
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface SavedRoute {
    id: string
    name: string
    start: RoutePlace
    destination: RoutePlace
    activityProfile: ActivityProfile
    departureTime: string
    arrivalTime: string
    distanceMeters: number
    durationSeconds: number
    geometry: RouteGeometry
    weatherScore: number
    weatherScoreVersion?: string
    thunderstormCapApplied?: boolean
    weatherSegments: SavedRouteWeatherSegment[]
    favorite: boolean
    createdAt: string
    updatedAt?: string
}

export interface UserError {
    field: string | null
    code: string
    message: string
}

/**
 * Verweist auf das Ergebnis einer Routenberechnung und der zugehörigen
 * Wetterbewertung. Beides wird für das Speichern benötigt.
 */
export interface CalculatedRouteReference {
    routeCalculationId: string
    weatherEvaluationId: string
}