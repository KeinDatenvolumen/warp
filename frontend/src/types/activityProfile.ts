export type ActivityProfileCode =
    | 'BICYCLE'
    | 'MOTORCYCLE'
    | 'HIKING'

export interface ActivityProfile {
    code: ActivityProfileCode
    displayName: string
    weatherQueryIntervalMinutes: number
}