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
                }
                destination {
                    label
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