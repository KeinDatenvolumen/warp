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