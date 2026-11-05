// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
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