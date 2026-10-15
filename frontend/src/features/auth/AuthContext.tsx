import {createContext, type PropsWithChildren, useCallback, useMemo, useState} from 'react'
import {clearAuthenticatedState, readAuthenticatedState, writeAuthenticatedState} from './authStorage'

export interface AuthContextValue {
    isAuthenticated: boolean
    login: () => void
    logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({children}: PropsWithChildren) {
    const [isAuthenticated, setIsAuthenticated] = useState(
        readAuthenticatedState
    )

    const login = useCallback(() => {
        writeAuthenticatedState(true)
        setIsAuthenticated(true)
    }, [])

    const logout = useCallback(() => {
        clearAuthenticatedState()
        setIsAuthenticated(false)
    }, [])

    const value = useMemo<AuthContextValue>(
        () => ({
            isAuthenticated,
            login,
            logout
        }),
        [isAuthenticated, login, logout]
    )

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}