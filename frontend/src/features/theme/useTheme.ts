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