// [KI-Log Nr. KI-206] (d2ddaae) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add theme structure with three color profiles and light/dark mode (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {useContext} from 'react'
import {ThemeContext} from './ThemeContext'

export function useTheme() {
    const context = useContext(ThemeContext)

    if (context === null) {
        throw new Error(
            'useTheme muss innerhalb eines ThemeProvider verwendet werden.'
        )
    }

    return context
}