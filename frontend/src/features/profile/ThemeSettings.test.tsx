// [KI-Log Nr. KI-207] (dc7f723) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add appearance settings to the profile page (WARP-033)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
    beforeEach,
    describe,
    expect,
    it,
} from 'vitest'
import {ThemeProvider} from '../theme/ThemeContext'
import ThemeSettings from './ThemeSettings'

function renderSettings() {
    return render(
        <ThemeProvider>
            <ThemeSettings />
        </ThemeProvider>,
    )
}

describe('ThemeSettings', () => {
    beforeEach(() => {
        window.localStorage.clear()

        delete document.documentElement.dataset.colorProfile
        delete document.documentElement.dataset.colorMode
    })

    it('bietet drei Farbprofile und drei Modi an', () => {
        renderSettings()

        expect(
            screen.getAllByRole('radio'),
        ).toHaveLength(6)

        expect(
            screen.getByRole('radio', {
                name: 'Natürlich und minimalistisch',
            }),
        ).toBeChecked()

        expect(
            screen.getByRole('radio', {
                name: 'Wie Gerät',
            }),
        ).toBeChecked()
    })

    it('wechselt Farbprofil und Modus', async () => {
        const user = userEvent.setup()

        renderSettings()

        await user.click(
            screen.getByRole('radio', {
                name: 'Gedämpft und hochwertig',
            }),
        )

        await user.click(
            screen.getByRole('radio', {
                name: 'Dunkel',
            }),
        )

        expect(
            document.documentElement.dataset.colorProfile,
        ).toBe('profile-2')

        expect(
            document.documentElement.dataset.colorMode,
        ).toBe('dark')

        expect(
            screen.getByRole('radio', {
                name: 'Gedämpft und hochwertig',
            }),
        ).toBeChecked()

        expect(
            screen.getByRole('radio', {
                name: 'Dunkel',
            }),
        ).toBeChecked()

        expect(
            JSON.parse(
                window.localStorage.getItem('warp.theme') ?? '{}',
            ),
        ).toEqual({
            profile: 'profile-2',
            mode: 'dark',
        })
    })
})