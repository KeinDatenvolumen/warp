// [KI-Log Nr. KI-186] (49cd50d) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Add routing calculation (WARP-001)
// [KI-Log Nr. KI-188] (c4f557c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-013): route calculation, departure time selection, and result display
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    gql,
    type TypedDocumentNode,
} from '@apollo/client'
import type {
    RouteCalculationQueryData,
    RouteCalculationQueryVariables,
    StartRouteCalculationMutationData,
    StartRouteCalculationMutationVariables,
} from '../types/routeCalculation'

export const START_ROUTE_CALCULATION_MUTATION: TypedDocumentNode<
    StartRouteCalculationMutationData,
    StartRouteCalculationMutationVariables
> = gql`
    mutation StartRouteCalculation(
        $input: RouteCalculationInput!
    ) {
        startRouteCalculation(input: $input) {
            calculation {
                id
                status
            }
            errors {
                field
                code
                message
            }
        }
    }
`

export const ROUTE_CALCULATION_QUERY: TypedDocumentNode<
    RouteCalculationQueryData,
    RouteCalculationQueryVariables
> = gql`
    query RouteCalculation($id: ID!) {
        routeCalculation(id: $id) {
            id
            status
            start {
                label
                latitude
                longitude
            }
            destination {
                label
                latitude
                longitude
            }
            activityProfile {
                code
                displayName
            }
            departureTime
            arrivalTime
            distanceMeters
            durationSeconds
            geometry {
                type
                coordinates
            }
            weatherEvaluationId
            weatherScore
            weatherScoreVersion
            thunderstormCapApplied
            weatherSegments {
                segmentIndex
                distanceFromStartMeters
                expectedArrivalTime
                score
                color
                limitingFactor
                reasonLabel
                weather {
                    windSpeedKmh
                    windDirectionDegrees
                    windGustsKmh
                    precipitationProbabilityPercent
                    precipitationMm
                    wetRoadIndex
                    thunderstormProbabilityPercent
                    temperatureCelsius
                    apparentTemperatureCelsius
                    weatherCode
                    limitingFactor
                    reasonLabel
                }
                geometry {
                    type
                    coordinates
                }
            }
            createdAt
            error {
                code
                message
            }
        }
    }
`
