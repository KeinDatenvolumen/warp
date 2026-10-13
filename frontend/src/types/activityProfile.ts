export type ActivityProfileCode =
    | 'BICYCLE'
    | 'MOTORCYCLE'
    | 'HIKING'

export type RoutingProfileCode =
    | 'BICYCLE'
    | 'MOTORCYCLE'
    | 'FOOT'

export interface WayPreferences {
    preferCycleways: boolean
    avoidMotorways: boolean
    avoidTollRoads: boolean
    avoidUnpavedWays: boolean
    preferPavedWays: boolean
    preferScenicWays: boolean
}

export interface ActivityProfile {
    code: ActivityProfileCode
    displayName: string
    averageSpeedKmh: number
    routingProfile: RoutingProfileCode
    weatherQueryIntervalMinutes: number
    wayPreferences: WayPreferences
}