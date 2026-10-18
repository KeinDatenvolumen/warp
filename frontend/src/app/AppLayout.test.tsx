/**
 * @vitest-environment jsdom
 */

import {render, screen, waitFor, within} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter, Route, Routes} from 'react-router'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {
    AuthContext,
    type AuthContextValue,
    type AuthStatus
} from '../features/auth/AuthContext'
import AppLayout from './AppLayout'

function authValue(
    status: AuthStatus,
    logout: AuthContextValue['logout'] = vi.fn().mockResolvedValue(undefined)
): AuthContextValue {
    return {
        status,
        session: null,
        isLoading: status === 'loading',
        isAuthenticated: status === 'authenticated',
        login: vi.fn(),
        logout
    }
}

function renderLayout(
    value: AuthContextValue,
    initialEntry = '/profile'
) {
    return render(
        <AuthContext.Provider value={value}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route element={<AppLayout/>}>
                        <Route path="/" element={<p>Startseiten-Inhalt</p>}/>
                        <Route path="/profile" element={<p>Profil-Inhalt</p>}/>
                    </Route>
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>
    )
}

async function openLogoutDialog(user: ReturnType<typeof userEvent.setup>) {
    await user.click(
        screen.getByRole('button', {name: 'Abmelden'})
    )

    return screen.getByRole('dialog', {name: 'Wirklich abmelden?'})
}

async function confirmLogout(user: ReturnType<typeof userEvent.setup>) {
    const dialog = await openLogoutDialog(user)

    await user.click(
        within(dialog).getByRole('button', {name: 'Abmelden'})
    )
}

describe('AppLayout', () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    describe('Navigation je nach Sitzungsstatus', () => {
        it('zeigt während der Sitzungsprüfung weder Anmelden noch Abmelden', () => {
            renderLayout(authValue('loading'))

            expect(
                screen.queryByRole('link', {name: 'Anmelden'})
            ).not.toBeInTheDocument()
            expect(
                screen.queryByRole('button', {name: 'Abmelden'})
            ).not.toBeInTheDocument()
            expect(
                screen.queryByRole('link', {name: 'Profil'})
            ).not.toBeInTheDocument()
        })

        it('zeigt abgemeldeten Benutzern den Link zur Anmeldung', () => {
            renderLayout(authValue('unauthenticated'))

            expect(
                screen.getByRole('link', {name: 'Anmelden'})
            ).toHaveAttribute('href', '/login')
            expect(
                screen.queryByRole('button', {name: 'Abmelden'})
            ).not.toBeInTheDocument()
            expect(
                screen.queryByRole('link', {name: 'Profil'})
            ).not.toBeInTheDocument()
            expect(
                screen.queryByRole('link', {name: 'Gespeicherte Routen'})
            ).not.toBeInTheDocument()
        })

        it('zeigt angemeldeten Benutzern geschützte Bereiche und die Abmeldung', () => {
            renderLayout(authValue('authenticated'))

            expect(
                screen.getByRole('link', {name: 'Profil'})
            ).toBeInTheDocument()
            expect(
                screen.getByRole('link', {name: 'Gespeicherte Routen'})
            ).toBeInTheDocument()
            expect(
                screen.getByRole('button', {name: 'Abmelden'})
            ).toBeInTheDocument()
            expect(
                screen.queryByRole('link', {name: 'Anmelden'})
            ).not.toBeInTheDocument()
        })
    })

    describe('Abmeldung', () => {
        it('zeigt zuerst eine Rückfrage und meldet noch nicht ab', async () => {
            const logout = vi.fn().mockResolvedValue(undefined)
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            const dialog = await openLogoutDialog(user)

            expect(dialog).toHaveTextContent('Wirklich abmelden?')
            expect(logout).not.toHaveBeenCalled()
        })

        it('fokussiert beim Öffnen die ungefährliche Option "Abbrechen"', async () => {
            renderLayout(authValue('authenticated'))
            const user = userEvent.setup()

            const dialog = await openLogoutDialog(user)

            expect(
                within(dialog).getByRole('button', {name: 'Abbrechen'})
            ).toHaveFocus()
        })

        it('meldet nach der Bestätigung ab und leitet zur öffentlichen Startseite weiter', async () => {
            const logout = vi.fn().mockResolvedValue(undefined)
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            await confirmLogout(user)

            await waitFor(() =>
                expect(screen.getByText('Startseiten-Inhalt')).toBeInTheDocument()
            )
            expect(logout).toHaveBeenCalledTimes(1)
        })

        it('meldet nicht ab, wenn "Abbrechen" gewählt wird', async () => {
            const logout = vi.fn().mockResolvedValue(undefined)
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            const dialog = await openLogoutDialog(user)
            await user.click(
                within(dialog).getByRole('button', {name: 'Abbrechen'})
            )

            expect(logout).not.toHaveBeenCalled()
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
            expect(screen.getByText('Profil-Inhalt')).toBeInTheDocument()
        })

        it('schließt den Dialog mit Esc, ohne abzumelden', async () => {
            const logout = vi.fn().mockResolvedValue(undefined)
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            const dialog = await openLogoutDialog(user)
            dialog.dispatchEvent(
                new Event('cancel', {bubbles: false, cancelable: true})
            )

            await waitFor(() =>
                expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
            )
            expect(logout).not.toHaveBeenCalled()
        })

        it('schließt den Dialog per Klick auf den Hintergrund', async () => {
            const logout = vi.fn().mockResolvedValue(undefined)
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            const dialog = await openLogoutDialog(user)
            await user.click(dialog)

            await waitFor(() =>
                expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
            )
            expect(logout).not.toHaveBeenCalled()
        })

        it('zeigt während der Abmeldung einen Ladezustand und verhindert Mehrfachaufrufe', async () => {
            let finishLogout!: () => void
            const logout = vi.fn(
                () =>
                    new Promise<void>((resolve) => {
                        finishLogout = resolve
                    })
            )
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            await confirmLogout(user)

            const busyButton = screen.getByRole('button', {
                name: 'Abmeldung läuft …'
            })
            expect(busyButton).toBeDisabled()
            expect(logout).toHaveBeenCalledTimes(1)

            finishLogout()

            await waitFor(() =>
                expect(screen.getByText('Startseiten-Inhalt')).toBeInTheDocument()
            )
        })

        it('zeigt einen Fehler und bleibt auf der Seite, wenn die Abmeldung fehlschlägt', async () => {
            const logout = vi.fn().mockRejectedValue(new Error('offline'))
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            await confirmLogout(user)

            expect(await screen.findByRole('alert')).toHaveTextContent(
                'Die Abmeldung konnte nicht abgeschlossen werden. Bitte versuche es erneut.'
            )
            expect(screen.getByText('Profil-Inhalt')).toBeInTheDocument()
            expect(
                screen.getByRole('button', {name: 'Abmelden'})
            ).toBeEnabled()
        })

        it('entfernt die Fehlermeldung beim nächsten Versuch', async () => {
            const logout = vi
                .fn()
                .mockRejectedValueOnce(new Error('offline'))
                .mockResolvedValueOnce(undefined)
            renderLayout(authValue('authenticated', logout))
            const user = userEvent.setup()

            await confirmLogout(user)
            await screen.findByRole('alert')

            await confirmLogout(user)

            await waitFor(() =>
                expect(screen.queryByRole('alert')).not.toBeInTheDocument()
            )
        })
    })
})