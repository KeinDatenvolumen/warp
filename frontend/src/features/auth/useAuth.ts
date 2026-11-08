// [KI-Log Nr. KI-031] (dffcf3f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-038] (8b1a41d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {useContext} from 'react'
import {AuthContext} from './AuthContext'

export function useAuth() {
    const context = useContext(AuthContext)

    if (context === null) {
        throw new Error(
            'useAuth muss innerhalb eines AuthProvider verwendet werden.'
        )
    }

    return context
}