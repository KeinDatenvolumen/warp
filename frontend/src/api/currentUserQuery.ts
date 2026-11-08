// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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