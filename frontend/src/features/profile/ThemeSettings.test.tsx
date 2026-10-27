/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {beforeEach, describe, expect, it} from 'vitest'
import {ThemeProvider} from '../theme/ThemeContext'
import ThemeSettings from './ThemeSettings'

function renderSettings() {
    return render(
        <ThemeProvider>
            <ThemeSettings/>
        </ThemeProvider>
    )
}

describe('ThemeSettings', () => {
    beforeEach(() => {
        window.localStorage.clear()
    })

    it('bietet drei Farbprofile und drei Modi an', () => {
        renderSettings()

        expect(screen.getAllByRole('radio')).toHaveLength(6)
        expect(
            screen.getByRole('radio', {name: 'Farbprofil 1'})
        ).toBeChecked()
        expect(screen.getByRole('radio', {name: 'Wie Gerät'})).toBeChecked()
    })

    it('wechselt Farbprofil und Modus', async () => {
        const user = userEvent.setup()

        renderSettings()

        await user.click(screen.getByRole('radio', {name: 'Farbprofil 2'}))
        await user.click(screen.getByRole('radio', {name: 'Dunkel'}))

        expect(document.documentElement.dataset.colorProfile).toBe('profile-2')
        expect(document.documentElement.dataset.colorMode).toBe('dark')
        expect(screen.getByRole('radio', {name: 'Farbprofil 2'})).toBeChecked()
        expect(screen.getByRole('radio', {name: 'Dunkel'})).toBeChecked()
    })
})