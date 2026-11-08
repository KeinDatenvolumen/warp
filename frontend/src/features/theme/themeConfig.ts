// [KI-Log Nr. KI-206] (d2ddaae) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add theme structure with three color profiles and light/dark mode (WARP-033)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-211] (d6d8b5b) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix(WARP-031): refine default theme and navigation design
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export const COLOR_PROFILES = [
    {
        id: 'profile-1',
        label: 'Lebendig und warm',
        description: 'Warme Orange-, Gelb- und Grüntöne',
    },
    {
        id: 'profile-2',
        label: 'Gedämpft und hochwertig',
        description: 'Ruhige Blau-, Braun- und Naturtöne',
    },
    {
        id: 'profile-3',
        label: 'Natürlich und minimalistisch',
        description: 'Reduzierte Grün- und neutrale Erdtöne',
    },
] as const

export const COLOR_MODES = [
    {
        id: 'light',
        label: 'Hell',
        description: 'Helle Darstellung verwenden',
    },
    {
        id: 'dark',
        label: 'Dunkel',
        description: 'Dunkle Darstellung verwenden',
    },
    {
        id: 'system',
        label: 'Wie Gerät',
        description: 'Einstellung des Betriebssystems übernehmen',
    },
] as const

export type ColorProfileId =
    (typeof COLOR_PROFILES)[number]['id']

export type ColorModePreference =
    (typeof COLOR_MODES)[number]['id']

export type ResolvedColorMode = 'light' | 'dark'

export interface ThemePreferences {
    profile: ColorProfileId
    mode: ColorModePreference
}

export const DEFAULT_THEME_PREFERENCES: ThemePreferences = {
    profile: 'profile-3',
    mode: 'system',
}

/**
 * Muss mit dem Inline-Skript in index.html übereinstimmen.
 */
export const THEME_STORAGE_KEY = 'warp.theme'

function isColorProfileId(value: unknown): value is ColorProfileId {
    return COLOR_PROFILES.some((profile) => profile.id === value)
}

function isColorModePreference(
    value: unknown,
): value is ColorModePreference {
    return COLOR_MODES.some((mode) => mode.id === value)
}

export function parseThemePreferences(
    raw: string | null | undefined,
): ThemePreferences {
    if (!raw) {
        return DEFAULT_THEME_PREFERENCES
    }

    try {
        const parsed: unknown = JSON.parse(raw)

        if (typeof parsed !== 'object' || parsed === null) {
            return DEFAULT_THEME_PREFERENCES
        }

        const {profile, mode} = parsed as Record<string, unknown>

        return {
            profile: isColorProfileId(profile)
                ? profile
                : DEFAULT_THEME_PREFERENCES.profile,
            mode: isColorModePreference(mode)
                ? mode
                : DEFAULT_THEME_PREFERENCES.mode,
        }
    } catch {
        return DEFAULT_THEME_PREFERENCES
    }
}

export function resolveColorMode(
    mode: ColorModePreference,
    systemPrefersDark: boolean,
): ResolvedColorMode {
    if (mode === 'system') {
        return systemPrefersDark ? 'dark' : 'light'
    }

    return mode
}