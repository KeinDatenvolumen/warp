// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
import {
    createContext,
    type PropsWithChildren,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState
} from 'react'
import {clearApolloCache} from '../../api/apolloClient'
import {
    createSession,
    deleteCurrentSession,
    fetchCurrentSession
} from '../../api/sessionApi'
import type {CurrentSession, LoginRequest} from '../../types/session'
import {SessionNotEstablishedError} from './authErrors'
import {subscribeToSessionInvalid} from './sessionEvents'

export type AuthStatus =
    | 'loading'
    | 'authenticated'
    | 'unauthenticated'

interface AuthState {
    status: AuthStatus
    session: CurrentSession | null
}

export interface AuthContextValue {
    status: AuthStatus
    session: CurrentSession | null
    isLoading: boolean
    isAuthenticated: boolean
    login: (credentials: LoginRequest) => Promise<void>
    logout: () => Promise<void>
}

const LOADING_STATE: AuthState = {status: 'loading', session: null}
const UNAUTHENTICATED_STATE: AuthState = {
    status: 'unauthenticated',
    session: null
}

export const AuthContext = createContext<AuthContextValue | null>(null)

function clearCachedUserData(): void {
    clearApolloCache().catch(() => undefined)
}

export function AuthProvider({children}: PropsWithChildren) {
    const [state, setState] = useState<AuthState>(LOADING_STATE)
    const stateVersion = useRef(0)

    useEffect(() => {
        const versionAtStart = stateVersion.current
        let active = true

        function applyStartupResult(next: AuthState): void {
            if (active && stateVersion.current === versionAtStart) {
                setState(next)
            }
        }

        fetchCurrentSession()
            .then((session) =>
                applyStartupResult(
                    session
                        ? {status: 'authenticated', session}
                        : UNAUTHENTICATED_STATE
                )
            )
            .catch(() => applyStartupResult(UNAUTHENTICATED_STATE))

        return () => {
            active = false
        }
    }, [])

    useEffect(
        () =>
            subscribeToSessionInvalid(() => {
                setState((current) => {
                    if (current.status === 'authenticated') {
                        stateVersion.current += 1
                        clearCachedUserData()
                        return UNAUTHENTICATED_STATE
                    }
                    return current
                })
            }),
        []
    )

    const login = useCallback(
        async (credentials: LoginRequest): Promise<void> => {
            await createSession(credentials)
            const session = await fetchCurrentSession()

            if (!session) {
                throw new SessionNotEstablishedError()
            }

            stateVersion.current += 1
            setState({status: 'authenticated', session})
        },
        []
    )

    const logout = useCallback(async (): Promise<void> => {
        await deleteCurrentSession()

        stateVersion.current += 1
        setState(UNAUTHENTICATED_STATE)
        clearCachedUserData()
    }, [])

    const value = useMemo<AuthContextValue>(
        () => ({
            status: state.status,
            session: state.session,
            isLoading: state.status === 'loading',
            isAuthenticated: state.status === 'authenticated',
            login,
            logout
        }),
        [state, login, logout]
    )

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}