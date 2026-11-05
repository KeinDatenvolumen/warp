// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
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
