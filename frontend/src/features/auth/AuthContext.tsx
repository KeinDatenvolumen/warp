// [KI-Log Nr. KI-031] (dffcf3f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-037] (6fdd913) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: added authentication state and protected routes (WARP-004)
// [KI-Log Nr. KI-038] (8b1a41d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-042] (9854cf8) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: change capitalisation of file (WARP-004)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-126] (8495425) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: manage session status centrally and implement login form (WARP-006)
// [KI-Log Nr. KI-234] (d6e6dbb) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): fix(frontend): prevent sessionInvalidLink and clearStore from cancelling startup queries when unauthenticated (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
import {
    subscribeToSessionInvalid,
    subscribeToSessionUpdated
} from './sessionEvents'

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
    refreshSession?: () => Promise<void>
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

    const refreshSession = useCallback(async (): Promise<void> => {
        try {
            const session = await fetchCurrentSession()
            stateVersion.current += 1
            if (session) {
                setState({status: 'authenticated', session})
                clearCachedUserData()
            } else {
                setState(UNAUTHENTICATED_STATE)
                clearCachedUserData()
            }
        } catch {
            // Bei vorübergehendem Netzwerkfehler aktuellen Status beibehalten
        }
    }, [])

    useEffect(
        () =>
            subscribeToSessionUpdated(() => {
                void refreshSession()
            }),
        [refreshSession]
    )

    useEffect(() => {
        if (
            state.status !== 'authenticated' ||
            state.session?.emailVerified !== false ||
            typeof window === 'undefined'
        ) {
            return
        }

        const handleWindowFocus = (): void => {
            void refreshSession()
        }

        const handleVisibilityChange = (): void => {
            if (document.visibilityState === 'visible') {
                void refreshSession()
            }
        }

        window.addEventListener('focus', handleWindowFocus)
        document.addEventListener('visibilitychange', handleVisibilityChange)

        return () => {
            window.removeEventListener('focus', handleWindowFocus)
            document.removeEventListener(
                'visibilitychange',
                handleVisibilityChange
            )
        }
    }, [state.status, state.session?.emailVerified, refreshSession])

    const value = useMemo<AuthContextValue>(
        () => ({
            status: state.status,
            session: state.session,
            isLoading: state.status === 'loading',
            isAuthenticated: state.status === 'authenticated',
            login,
            logout,
            refreshSession
        }),
        [state, login, logout, refreshSession]
    )

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}