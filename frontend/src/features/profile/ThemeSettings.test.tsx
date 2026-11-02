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