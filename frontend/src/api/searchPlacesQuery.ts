// [KI-Log Nr. KI-098] (f9cda8a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add place search inputs and GraphQL query (WARP-010)
// [KI-Log Nr. KI-131] (a369159) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add place search inputs and GraphQL query (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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