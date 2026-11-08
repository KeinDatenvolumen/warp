// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-198] (884c88d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add favorite API operations and toggle button (WARP-022)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-242] (a913a41) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: optimiere Routenplaner, Abfahrtszeit-Slider und gespeicherte Routen (WARP-014)
import {gql, type TypedDocumentNode} from '@apollo/client'
import type {
    PageInfo,
    SavedRoute,
    SavedRouteSummary,
    UserError
} from '../types/savedRoute'

export interface SavedRoutesQueryData {
    savedRoutes: {
        items: SavedRouteSummary[]
        pageInfo: PageInfo
    }
}

export interface SavedRoutesQueryVariables {
    filter?: { favorite: boolean }
    page?: { page: number; size: number }
}

export const SAVED_ROUTES_QUERY: TypedDocumentNode<
    SavedRoutesQueryData,
    SavedRoutesQueryVariables
> = gql`
    query SavedRoutes($filter: SavedRouteFilter, $page: PageInput) {
        savedRoutes(filter: $filter, page: $page) {
            items {
                id
                name
                start {
                    label
                    latitude
                    longitude
                }
                destination {
                    label
                    latitude
                    longitude
                }
                activityProfile {
                    code
                    displayName
                }
                weatherScore
                favorite
                createdAt
            }
            pageInfo {
                page
                size
                totalElements
                totalPages
            }
        }
    }
`

export interface SavedRouteQueryData {
    savedRoute: SavedRoute | null
}

export interface SavedRouteQueryVariables {
    id: string
}

export const SAVED_ROUTE_QUERY: TypedDocumentNode<
    SavedRouteQueryData,
    SavedRouteQueryVariables
> = gql`
    query SavedRoute($id: ID!) {
        savedRoute(id: $id) {
            id
            name
            start {
                label
                latitude
                longitude
            }
            destination {
                label
                latitude
                longitude
            }
            activityProfile {
                code
                displayName
            }
            departureTime
            arrivalTime
            distanceMeters
            durationSeconds
            geometry {
                type
                coordinates
            }
            weatherScore
            weatherSegments {
                segmentIndex
                score
                color
                geometry {
                    type
                    coordinates
                }
            }
            favorite
            createdAt
        }
    }
`

export interface DeleteSavedRouteMutationData {
    deleteSavedRoute: {
        deletedRouteId: string | null
        errors: Pick<UserError, 'code' | 'message'>[]
    }
}

export const DELETE_SAVED_ROUTE_MUTATION: TypedDocumentNode<
    DeleteSavedRouteMutationData,
    { id: string }
> = gql`
    mutation DeleteSavedRoute($id: ID!) {
        deleteSavedRoute(id: $id) {
            deletedRouteId
            errors {
                code
                message
            }
        }
    }
`

export interface SaveRouteInput {
    name: string
    routeCalculationId: string
    weatherEvaluationId: string
    favorite: boolean
}

export interface SaveRouteMutationData {
    saveRoute: {
        route: {
            id: string
            name: string
            favorite: boolean
            createdAt: string
        } | null
        errors: UserError[]
    }
}

export const SAVE_ROUTE_MUTATION: TypedDocumentNode<
    SaveRouteMutationData,
    { input: SaveRouteInput }
> = gql`
    mutation SaveRoute($input: SaveRouteInput!) {
        saveRoute(input: $input) {
            route {
                id
                name
                favorite
                createdAt
            }
            errors {
                field
                code
                message
            }
        }
    }
`

export interface UpdateSavedRouteMutationData {
    updateSavedRoute: {
        route: {
            id: string
            favorite: boolean
            updatedAt: string
        } | null
        errors: UserError[]
    }
}

export const UPDATE_SAVED_ROUTE_MUTATION: TypedDocumentNode<
    UpdateSavedRouteMutationData,
    { id: string; input: { name?: string; favorite?: boolean } }
> = gql`
    mutation UpdateSavedRoute($id: ID!, $input: UpdateSavedRouteInput!) {
        updateSavedRoute(id: $id, input: $input) {
            route {
                id
                favorite
                updatedAt
            }
            errors {
                field
                code
                message
            }
        }
    }
`