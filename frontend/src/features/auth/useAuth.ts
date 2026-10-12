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