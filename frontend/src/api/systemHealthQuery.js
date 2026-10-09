import {gql} from '@apollo/client'

export const SYSTEM_HEALTH_QUERY = gql`
    query SystemHealth {
        systemHealth {
            status
            checkedAt
            healthyServices
            totalServices
            services {
                service
                status
                reachable
                responseTimeMs
            }
        }
    }
`