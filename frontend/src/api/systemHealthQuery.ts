import {gql, type TypedDocumentNode} from '@apollo/client'
import type {SystemHealthQueryData} from '../types/systemHealth'

export const SYSTEM_HEALTH_QUERY: TypedDocumentNode<
    SystemHealthQueryData
> = gql`
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