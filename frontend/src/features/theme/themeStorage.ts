// [KI-Log Nr. KI-206] (d2ddaae) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add theme structure with three color profiles and light/dark mode (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    DEFAULT_THEME_PREFERENCES,
    THEME_STORAGE_KEY,
    parseThemePreferences,
    type ThemePreferences
} from './themeConfig'

export function loadThemePreferences(): ThemePreferences {
    try {
        return parseThemePreferences(
            window.localStorage.getItem(THEME_STORAGE_KEY)
        )
    } catch {
        return DEFAULT_THEME_PREFERENCES
    }
}

export function saveThemePreferences(preferences: ThemePreferences): void {
    try {
        window.localStorage.setItem(
            THEME_STORAGE_KEY,
            JSON.stringify(preferences)
        )
    } catch {
        // Speicher nicht verfügbar (z. B. privater Modus): Auswahl gilt nur für diese Sitzung.
    }
}