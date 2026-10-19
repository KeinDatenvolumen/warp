import { gql, type TypedDocumentNode } from '@apollo/client'
import type {
    SearchPlacesQueryData,
    SearchPlacesQueryVariables,
} from '../types/searchPlaces'

export const SEARCH_PLACES_QUERY: TypedDocumentNode<
    SearchPlacesQueryData,
    SearchPlacesQueryVariables
> = gql`
    query SearchPlaces($query: String!, $limit: Int = 5) {
        searchPlaces(query: $query, limit: $limit) {
            placeId
            displayName
            latitude
            longitude
        }
    }
`