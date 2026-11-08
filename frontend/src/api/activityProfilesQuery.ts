// [KI-Log Nr. KI-064] (f2841c9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend activity profile API (WARP-033)
// [KI-Log Nr. KI-065] (73f94e9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add activity profile selection to route planner (WARP-033)
// [KI-Log Nr. KI-074] (cfd4246) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose individual activity profile query (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {gql, type TypedDocumentNode} from '@apollo/client'
import type {ActivityProfile,ActivityProfileCode} from '../types/activityProfile'

export interface ActivityProfilesQueryData {
    activityProfiles: ActivityProfile[]
}

export interface ActivityProfileQueryVariables {
    code: ActivityProfileCode
}

export interface ActivityProfileQueryData {
    activityProfile: ActivityProfile
}

export const ACTIVITY_PROFILES_QUERY: TypedDocumentNode<
    ActivityProfilesQueryData
> = gql`
    query ActivityProfiles {
        activityProfiles {
            code
            displayName
        }
    }
`

export const ACTIVITY_PROFILE_QUERY: TypedDocumentNode<
    ActivityProfileQueryData,
    ActivityProfileQueryVariables
> = gql`
    query GetActivityProfile($code: ActivityProfileCode!) {
        activityProfile(code: $code) {
            code
            displayName
        }
    }
`