// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
import {gql, type TypedDocumentNode} from '@apollo/client'
import type {CurrentUser} from '../types/user'

export interface CurrentUserQueryData {
    currentUser: CurrentUser | null
}

export const CURRENT_USER_QUERY: TypedDocumentNode<CurrentUserQueryData> = gql`
    query CurrentUser {
        currentUser {
            id
            name
            email
            emailVerified
            createdAt
        }
    }
`