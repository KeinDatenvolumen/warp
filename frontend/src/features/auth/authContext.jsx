import {
    createContext,
    useCallback,
    useMemo,
    useState
} from 'react'
import {
    clearAuthenticatedState,
    readAuthenticatedState,
    writeAuthenticatedState
} from './authStorage.js'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
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

    const value = useMemo(
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