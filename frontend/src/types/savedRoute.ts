// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Favoriten-Funktion für gespeicherte Routen (WARP-022)
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