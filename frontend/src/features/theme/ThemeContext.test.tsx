// [KI-Log Nr. KI-206] (d2ddaae) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add theme structure with three color profiles and light/dark mode (WARP-033)
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {beforeEach, describe, expect, it} from 'vitest'
import {ThemeProvider} from './ThemeContext'
import {THEME_STORAGE_KEY} from './themeConfig'
import {useTheme} from './useTheme'

function Probe() {
    const {profile, mode, resolvedMode, setProfile, setMode} = useTheme()

    return (
        <>
            <p data-testid="state">
                {profile}/{mode}/{resolvedMode}
            </p>
            <button type="button" onClick={() => setProfile('profile-2')}>
                Profil 2
            </button>
            <button type="button" onClick={() => setMode('dark')}>
                Dunkel
            </button>
        </>
    )
}

function renderProbe() {
    return render(
        <ThemeProvider>
            <Probe/>
        </ThemeProvider>
    )
}

describe('ThemeProvider', () => {
    beforeEach(() => {
        window.localStorage.clear()
        delete document.documentElement.dataset.colorProfile
        delete document.documentElement.dataset.colorMode
    })

    it('setzt die Standardwerte auf das html-Element', () => {
        renderProbe()

        expect(document.documentElement.dataset.colorProfile).toBe('profile-3')
        expect(document.documentElement.dataset.colorMode).toBe('light')
    })

    it('liest eine gespeicherte Auswahl', () => {
        window.localStorage.setItem(
            THEME_STORAGE_KEY,
            JSON.stringify({profile: 'profile-3', mode: 'dark'})
        )

        renderProbe()

        expect(screen.getByTestId('state')).toHaveTextContent(
            'profile-3/dark/dark'
        )
        expect(document.documentElement.dataset.colorProfile).toBe('profile-3')
        expect(document.documentElement.dataset.colorMode).toBe('dark')
    })

    it('übernimmt Änderungen und speichert sie', async () => {
        const user = userEvent.setup()

        renderProbe()

        await user.click(screen.getByRole('button', {name: 'Profil 2'}))
        await user.click(screen.getByRole('button', {name: 'Dunkel'}))

        expect(document.documentElement.dataset.colorProfile).toBe('profile-2')
        expect(document.documentElement.dataset.colorMode).toBe('dark')
        expect(
            JSON.parse(
                window.localStorage.getItem(THEME_STORAGE_KEY) ?? '{}'
            )
        ).toEqual({profile: 'profile-2', mode: 'dark'})
    })
})