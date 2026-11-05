// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
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