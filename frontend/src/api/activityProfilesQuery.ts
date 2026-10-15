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
            weatherQueryIntervalMinutes
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
            weatherQueryIntervalMinutes
        }
    }
`