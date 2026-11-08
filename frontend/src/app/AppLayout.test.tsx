// [KI-Log Nr. KI-127] (2b97154) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add logout action with loading state, error handling and redirect (WARP-006)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
/**
 * @vitest-environment jsdom
 */

import {
    act,
    render,
    screen,
    waitFor,
    within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter, Route, Routes} from 'react-router'
import {
    afterEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'
import {
    AuthContext,
    type AuthContextValue,
    type AuthStatus,
} from '../features/auth/AuthContext'
import AppLayout from './AppLayout'

type User = ReturnType<typeof userEvent.setup>

function authValue(
    status: AuthStatus,
    logout: AuthContextValue['logout'] = vi
        .fn()
        .mockResolvedValue(undefined),
): AuthContextValue {
    return {
        status,
        session: null,
        isLoading: status === 'loading',
        isAuthenticated: status === 'authenticated',
        login: vi.fn(),
        logout,
    }
}

function renderLayout(
    value: AuthContextValue,
    initialEntry = '/profile',
) {
    return render(
        <AuthContext.Provider value={value}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route element={<AppLayout />}>
                        <Route
                            path="/"
                            element={<p>Startseiten-Inhalt</p>}
                        />
                        <Route
                            path="/profile"
                            element={<p>Profil-Inhalt</p>}
                        />
                    </Route>
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>,
    )
}

async function openNavigation(user: User): Promise<HTMLElement> {
    const openButton = screen.getByRole('button', {
        name: 'Navigation öffnen',
    })

    await user.click(openButton)

    expect(openButton).toHaveAttribute('aria-expanded', 'true')

    return screen.getByRole('navigation', {
        name: 'Hauptnavigation',
    })
}

async function openLogoutDialog(user: User): Promise<HTMLElement> {
    const existingDialog = screen.queryByRole('dialog', {
        name: 'Wirklich abmelden?',
    })

    if (existingDialog) {
        return existingDialog
    }

    const navigation = await openNavigation(user)

    await user.click(
        within(navigation).getByRole('button', {
            name: 'Abmelden',
        }),
    )

    return screen.getByRole('dialog', {
        name: 'Wirklich abmelden?',
    })
}

async function confirmLogout(user: User): Promise<void> {
    const dialog = await openLogoutDialog(user)

    await user.click(
        within(dialog).getByRole('button', {
            name: 'Abmelden',
        }),
    )
}

describe('AppLayout', () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    describe('Navigation je nach Sitzungsstatus', () => {
        it(
            'zeigt während der Sitzungsprüfung weder Anmelden noch Abmelden',
            async () => {
                renderLayout(authValue('loading'))
                const user = userEvent.setup()

                const navigation = await openNavigation(user)

                expect(
                    within(navigation).queryByRole('link', {
                        name: 'Anmelden',
                    }),
                ).not.toBeInTheDocument()

                expect(
                    within(navigation).queryByRole('button', {
                        name: 'Abmelden',
                    }),
                ).not.toBeInTheDocument()

                expect(
                    within(navigation).queryByRole('link', {
                        name: 'Profil',
                    }),
                ).not.toBeInTheDocument()
            },
        )

        it(
            'zeigt abgemeldeten Benutzern den Link zur Anmeldung',
            async () => {
                renderLayout(authValue('unauthenticated'))
                const user = userEvent.setup()

                const navigation = await openNavigation(user)

                expect(
                    within(navigation).getByRole('link', {
                        name: 'Anmelden',
                    }),
                ).toHaveAttribute('href', '/login')

                expect(
                    within(navigation).queryByRole('button', {
                        name: 'Abmelden',
                    }),
                ).not.toBeInTheDocument()

                expect(
                    within(navigation).queryByRole('link', {
                        name: 'Profil',
                    }),
                ).not.toBeInTheDocument()

                expect(
                    within(navigation).queryByRole('link', {
                        name: 'Gespeicherte Routen',
                    }),
                ).not.toBeInTheDocument()
            },
        )

        it(
            'zeigt angemeldeten Benutzern geschützte Bereiche und die Abmeldung',
            async () => {
                renderLayout(authValue('authenticated'))
                const user = userEvent.setup()

                const navigation = await openNavigation(user)

                expect(
                    within(navigation).getByRole('link', {
                        name: 'Profil',
                    }),
                ).toBeInTheDocument()

                expect(
                    within(navigation).getByRole('link', {
                        name: 'Gespeicherte Routen',
                    }),
                ).toBeInTheDocument()

                expect(
                    within(navigation).getByRole('button', {
                        name: 'Abmelden',
                    }),
                ).toBeInTheDocument()

                expect(
                    within(navigation).queryByRole('link', {
                        name: 'Anmelden',
                    }),
                ).not.toBeInTheDocument()
            },
        )

        it(
            'blendet Gespeicherte Routen für angemeldete, aber nicht verifizierte Benutzer aus',
            async () => {
                renderLayout({
                    ...authValue('authenticated'),
                    session: {
                        userId: 'usr_unverified',
                        createdAt: '2026-10-18T08:00:00Z',
                        expiresAt: '2026-10-25T08:00:00Z',
                        emailVerified: false,
                    },
                })
                const user = userEvent.setup()

                const navigation = await openNavigation(user)

                expect(
                    within(navigation).getByRole('link', {
                        name: 'Profil',
                    }),
                ).toBeInTheDocument()

                expect(
                    within(navigation).queryByRole('link', {
                        name: 'Gespeicherte Routen',
                    }),
                ).not.toBeInTheDocument()
            },
        )
    })

    describe('Abmeldung', () => {
        it(
            'zeigt zuerst eine Rückfrage und meldet noch nicht ab',
            async () => {
                const logout = vi
                    .fn()
                    .mockResolvedValue(undefined)

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()
                const dialog = await openLogoutDialog(user)

                expect(dialog).toHaveTextContent(
                    'Wirklich abmelden?',
                )
                expect(logout).not.toHaveBeenCalled()
            },
        )

        it(
            'fokussiert beim Öffnen die ungefährliche Option "Abbrechen"',
            async () => {
                renderLayout(authValue('authenticated'))
                const user = userEvent.setup()

                const dialog = await openLogoutDialog(user)
                const cancelButton = within(dialog).getByRole(
                    'button',
                    {
                        name: 'Abbrechen',
                    },
                )

                await waitFor(() => {
                    expect(cancelButton).toHaveFocus()
                })
            },
        )

        it(
            'meldet nach der Bestätigung ab und leitet zur öffentlichen Startseite weiter',
            async () => {
                const logout = vi
                    .fn()
                    .mockResolvedValue(undefined)

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()

                await confirmLogout(user)

                await waitFor(() => {
                    expect(
                        screen.getByText('Startseiten-Inhalt'),
                    ).toBeInTheDocument()
                })

                expect(logout).toHaveBeenCalledTimes(1)
            },
        )

        it(
            'meldet nicht ab, wenn "Abbrechen" gewählt wird',
            async () => {
                const logout = vi
                    .fn()
                    .mockResolvedValue(undefined)

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()
                const dialog = await openLogoutDialog(user)

                await user.click(
                    within(dialog).getByRole('button', {
                        name: 'Abbrechen',
                    }),
                )

                expect(logout).not.toHaveBeenCalled()

                expect(
                    screen.queryByRole('dialog'),
                ).not.toBeInTheDocument()

                expect(
                    screen.getByText('Profil-Inhalt'),
                ).toBeInTheDocument()
            },
        )

        it(
            'schließt den Dialog mit Esc, ohne abzumelden',
            async () => {
                const logout = vi
                    .fn()
                    .mockResolvedValue(undefined)

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()
                const dialog = await openLogoutDialog(user)

                act(() => {
                    dialog.dispatchEvent(
                        new Event('cancel', {
                            bubbles: false,
                            cancelable: true,
                        }),
                    )
                })

                await waitFor(() => {
                    expect(
                        screen.queryByRole('dialog'),
                    ).not.toBeInTheDocument()
                })

                expect(logout).not.toHaveBeenCalled()
            },
        )

        it(
            'schließt den Dialog per Klick auf den Hintergrund',
            async () => {
                const logout = vi
                    .fn()
                    .mockResolvedValue(undefined)

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()
                const dialog = await openLogoutDialog(user)

                await user.click(dialog)

                await waitFor(() => {
                    expect(
                        screen.queryByRole('dialog'),
                    ).not.toBeInTheDocument()
                })

                expect(logout).not.toHaveBeenCalled()
            },
        )

        it(
            'zeigt während der Abmeldung einen Ladezustand und verhindert Mehrfachaufrufe',
            async () => {
                let finishLogout!: () => void

                const logout = vi.fn(
                    () =>
                        new Promise<void>((resolve) => {
                            finishLogout = resolve
                        }),
                )

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()

                await confirmLogout(user)

                const busyButton = screen
                    .getByText('Abmeldung läuft …')
                    .closest('button')

                expect(busyButton).not.toBeNull()

                expect(busyButton).toBeDisabled()
                expect(busyButton).toHaveAttribute(
                    'aria-busy',
                    'true',
                )
                expect(logout).toHaveBeenCalledTimes(1)

                act(() => {
                    finishLogout()
                })

                await waitFor(() => {
                    expect(
                        screen.getByText('Startseiten-Inhalt'),
                    ).toBeInTheDocument()
                })
            },
        )

        it(
            'zeigt einen Fehler und bleibt auf der Seite, wenn die Abmeldung fehlschlägt',
            async () => {
                const logout = vi
                    .fn()
                    .mockRejectedValue(new Error('offline'))

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()

                await confirmLogout(user)

                expect(
                    await screen.findByRole('alert'),
                ).toHaveTextContent(
                    'Die Abmeldung konnte nicht abgeschlossen werden. Bitte versuche es erneut.',
                )

                expect(
                    screen.getByText('Profil-Inhalt'),
                ).toBeInTheDocument()

                expect(
                    screen.queryByRole('dialog', {
                        name: 'Wirklich abmelden?',
                    }),
                ).not.toBeInTheDocument()

                const navigation = await openNavigation(user)

                expect(
                    within(navigation).getByRole('button', {
                        name: 'Abmelden',
                    }),
                ).toBeEnabled()

                expect(logout).toHaveBeenCalledTimes(1)
            },
        )

        it(
            'entfernt die Fehlermeldung beim nächsten Versuch',
            async () => {
                const logout = vi
                    .fn()
                    .mockRejectedValueOnce(
                        new Error('offline'),
                    )
                    .mockResolvedValueOnce(undefined)

                renderLayout(
                    authValue('authenticated', logout),
                )

                const user = userEvent.setup()

                await confirmLogout(user)
                await screen.findByRole('alert')

                /*
                 * Nach dem fehlgeschlagenen Versuch ist der Dialog
                 * weiterhin geöffnet. confirmLogout verwendet deshalb
                 * den vorhandenen Dialog für den zweiten Versuch.
                 */
                await confirmLogout(user)

                await waitFor(() => {
                    expect(
                        screen.queryByRole('alert'),
                    ).not.toBeInTheDocument()
                })

                expect(logout).toHaveBeenCalledTimes(2)
            },
        )
    })
})