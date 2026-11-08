// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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