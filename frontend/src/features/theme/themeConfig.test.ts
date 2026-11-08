// [KI-Log Nr. KI-206] (d2ddaae) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add theme structure with three color profiles and light/dark mode (WARP-033)
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {
    DEFAULT_THEME_PREFERENCES,
    parseThemePreferences,
    resolveColorMode
} from './themeConfig'

describe('parseThemePreferences', () => {
    it('liefert Standardwerte ohne gespeicherte Auswahl', () => {
        expect(parseThemePreferences(null)).toEqual(DEFAULT_THEME_PREFERENCES)
    })

    it('liest gültige Werte', () => {
        expect(
            parseThemePreferences('{"profile":"profile-3","mode":"dark"}')
        ).toEqual({profile: 'profile-3', mode: 'dark'})
    })

    it('ersetzt ungültige Werte einzeln durch Standardwerte', () => {
        expect(
            parseThemePreferences('{"profile":"gibt-es-nicht","mode":"dark"}')
        ).toEqual({profile: 'profile-3', mode: 'dark'})
    })

    it('fängt kaputtes JSON ab', () => {
        expect(parseThemePreferences('{kaputt')).toEqual(
            DEFAULT_THEME_PREFERENCES
        )
    })
})

describe('resolveColorMode', () => {
    it('verwendet feste Modi unverändert', () => {
        expect(resolveColorMode('light', true)).toBe('light')
        expect(resolveColorMode('dark', false)).toBe('dark')
    })

    it('folgt bei "system" der Geräteeinstellung', () => {
        expect(resolveColorMode('system', true)).toBe('dark')
        expect(resolveColorMode('system', false)).toBe('light')
    })
})