// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
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