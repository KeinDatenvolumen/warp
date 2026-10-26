import { gql } from '@apollo/client'

export const START_ROUTE_CALCULATION_MUTATION = gql`
    mutation StartRouteCalculation($input: RouteCalculationInput!) {
        startRouteCalculation(input: $input) {
            calculation {
                id
                status
                distanceMeters
                durationSeconds
                geometry {
                    type
                    coordinates
                }
            }
            errors {
                field
                code
                message
            }
        }
    }
`

export const GET_ROUTE_CALCULATION_QUERY = gql`
    query GetRouteCalculation($id: ID!) {
        routeCalculation(id: $id) {
            id
            status
            distanceMeters
            durationSeconds
            geometry {
                type
                coordinates
            }
        }
    }
`
