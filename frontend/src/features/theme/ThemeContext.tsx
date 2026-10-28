import {
    createContext,
    type PropsWithChildren,
    useCallback,
    useEffect,
    useMemo,
    useState
} from 'react'
import {
    resolveColorMode,
    type ColorModePreference,
    type ColorProfileId,
    type ResolvedColorMode,
    type ThemePreferences
} from './themeConfig'
import {loadThemePreferences, saveThemePreferences} from './themeStorage'

export interface ThemeContextValue {
    profile: ColorProfileId
    mode: ColorModePreference
    resolvedMode: ResolvedColorMode
    setProfile: (profile: ColorProfileId) => void
    setMode: (mode: ColorModePreference) => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

const DARK_MEDIA_QUERY = '(prefers-color-scheme: dark)'

function readSystemPrefersDark(): boolean {
    return (
        typeof window.matchMedia === 'function' &&
        window.matchMedia(DARK_MEDIA_QUERY).matches
    )
}

export function ThemeProvider({children}: PropsWithChildren) {
    const [preferences, setPreferences] =
        useState<ThemePreferences>(loadThemePreferences)
    const [systemPrefersDark, setSystemPrefersDark] =
        useState<boolean>(readSystemPrefersDark)

    useEffect(() => {
        if (typeof window.matchMedia !== 'function') {
            return
        }

        const query = window.matchMedia(DARK_MEDIA_QUERY)
        const handleChange = (event: MediaQueryListEvent): void =>
            setSystemPrefersDark(event.matches)

        query.addEventListener('change', handleChange)

        return () => query.removeEventListener('change', handleChange)
    }, [])

    const resolvedMode = resolveColorMode(
        preferences.mode,
        systemPrefersDark
    )

    useEffect(() => {
        const root = document.documentElement

        root.dataset.colorProfile = preferences.profile
        root.dataset.colorMode = resolvedMode
    }, [preferences.profile, resolvedMode])

    const update = useCallback((next: ThemePreferences): void => {
        setPreferences(next)
        saveThemePreferences(next)
    }, [])

    const setProfile = useCallback(
        (profile: ColorProfileId): void => update({...preferences, profile}),
        [preferences, update]
    )

    const setMode = useCallback(
        (mode: ColorModePreference): void => update({...preferences, mode}),
        [preferences, update]
    )

    const value = useMemo<ThemeContextValue>(
        () => ({
            profile: preferences.profile,
            mode: preferences.mode,
            resolvedMode,
            setProfile,
            setMode
        }),
        [preferences, resolvedMode, setProfile, setMode]
    )

    return (
        <ThemeContext.Provider value={value}>
            {children}
        </ThemeContext.Provider>
    )
}