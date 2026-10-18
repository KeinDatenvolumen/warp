import {gql, type TypedDocumentNode} from '@apollo/client'
import type {CurrentSession} from '../types/session'

export interface CurrentSessionQueryData {
    currentSession: CurrentSession
}

export const CURRENT_SESSION_QUERY: TypedDocumentNode<
    CurrentSessionQueryData
> = gql`
    query CurrentSession {
        currentSession {
            userId
            createdAt
            expiresAt
        }
    }
`