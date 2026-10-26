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