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
        ).toEqual({profile: 'profile-1', mode: 'dark'})
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