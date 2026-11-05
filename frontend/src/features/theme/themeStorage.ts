// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
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