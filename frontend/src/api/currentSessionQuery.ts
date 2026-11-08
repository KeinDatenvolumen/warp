// [KI-Log Nr. KI-120] (61da337) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add session API with CSRF handling and Retry-After support (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
            emailVerified
        }
    }
`