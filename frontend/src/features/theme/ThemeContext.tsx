// [KI-Log Nr. KI-206] (d2ddaae) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add theme structure with three color profiles and light/dark mode (WARP-033)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    createContext,
    type PropsWithChildren,
    useCallback,
    useEffect,
    useMemo,
    useState,
} from 'react'
import {
    resolveColorMode,
    type ColorModePreference,
    type ColorProfileId,
    type ResolvedColorMode,
    type ThemePreferences,
} from './themeConfig'
import {
    loadThemePreferences,
    saveThemePreferences,
} from './themeStorage'

export interface ThemeContextValue {
    profile: ColorProfileId
    mode: ColorModePreference
    resolvedMode: ResolvedColorMode
    setProfile: (profile: ColorProfileId) => void
    setMode: (mode: ColorModePreference) => void
}

export const ThemeContext =
    createContext<ThemeContextValue | null>(null)

const DARK_MEDIA_QUERY = '(prefers-color-scheme: dark)'

function readSystemPrefersDark(): boolean {
    return (
        typeof window.matchMedia === 'function' &&
        window.matchMedia(DARK_MEDIA_QUERY).matches
    )
}

export function ThemeProvider({
                                  children,
                              }: PropsWithChildren) {
    const [preferences, setPreferences] =
        useState<ThemePreferences>(loadThemePreferences)

    const [systemPrefersDark, setSystemPrefersDark] =
        useState<boolean>(readSystemPrefersDark)

    useEffect(() => {
        if (typeof window.matchMedia !== 'function') {
            return
        }

        const query = window.matchMedia(DARK_MEDIA_QUERY)

        const handleChange = (
            event: MediaQueryListEvent,
        ): void => {
            setSystemPrefersDark(event.matches)
        }

        query.addEventListener('change', handleChange)

        return () => {
            query.removeEventListener('change', handleChange)
        }
    }, [])

    const resolvedMode = resolveColorMode(
        preferences.mode,
        systemPrefersDark,
    )

    useEffect(() => {
        const root = document.documentElement

        root.dataset.colorProfile = preferences.profile
        root.dataset.colorMode = resolvedMode
    }, [preferences.profile, resolvedMode])

    const setProfile = useCallback(
        (profile: ColorProfileId): void => {
            setPreferences((currentPreferences) => {
                const nextPreferences: ThemePreferences = {
                    ...currentPreferences,
                    profile,
                }

                saveThemePreferences(nextPreferences)

                return nextPreferences
            })
        },
        [],
    )

    const setMode = useCallback(
        (mode: ColorModePreference): void => {
            setPreferences((currentPreferences) => {
                const nextPreferences: ThemePreferences = {
                    ...currentPreferences,
                    mode,
                }

                saveThemePreferences(nextPreferences)

                return nextPreferences
            })
        },
        [],
    )

    const value = useMemo<ThemeContextValue>(
        () => ({
            profile: preferences.profile,
            mode: preferences.mode,
            resolvedMode,
            setProfile,
            setMode,
        }),
        [
            preferences.profile,
            preferences.mode,
            resolvedMode,
            setProfile,
            setMode,
        ],
    )

    return (
        <ThemeContext.Provider value={value}>
            {children}
        </ThemeContext.Provider>
    )
}