import {gql, type TypedDocumentNode} from '@apollo/client'
import type {ActivityProfile} from '../types/activityProfile'

export interface ActivityProfilesQueryData {
    activityProfiles: ActivityProfile[]
}

export const ACTIVITY_PROFILES_QUERY: TypedDocumentNode<
    ActivityProfilesQueryData
> = gql`
    query ActivityProfiles {
        activityProfiles {
            code
            displayName
            averageSpeedKmh
            routingProfile
            weatherQueryIntervalMinutes
            wayPreferences {
                preferCycleways
                avoidMotorways
                avoidTollRoads
                avoidUnpavedWays
                preferPavedWays
                preferScenicWays
            }
        }
    }
`