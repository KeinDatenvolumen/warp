// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
/**
 * @vitest-environment jsdom
 */

import {render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter, Route, Routes} from 'react-router'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {clearApolloCache} from '../api/apolloClient'
import {ApiRequestError} from '../api/restClient'
import {
    createSession,
    fetchCurrentSession
} from '../api/sessionApi'
import ProtectedRoute from '../components/routing/ProtectedRoute'
import {AuthProvider} from '../features/auth/AuthContext'
import type {CurrentSession} from '../types/session'
import LoginPage from './LoginPage'

vi.mock('../api/sessionApi', () => ({
    createSession: vi.fn(),
    deleteCurrentSession: vi.fn(),
    fetchCurrentSession: vi.fn()
}))

vi.mock('../api/apolloClient', () => ({
    clearApolloCache: vi.fn()
}))

const mockedCreateSession = vi.mocked(createSession)
const mockedFetchCurrentSession = vi.mocked(fetchCurrentSession)

const session: CurrentSession = {
    userId: 'usr_123',
    createdAt: '2026-10-18T08:00:00Z',
    expiresAt: '2026-10-25T08:00:00Z'
}

const PASSWORD = 'MeinSicheresPasswort123'

function renderLoginPage(initialEntry = '/login') {
    return render(
        <AuthProvider>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route
                        path="/route-planner"
                        element={<p>Routenplaner-Seite</p>}
                    />
                    <Route
                        path="/profile"
                        element={
                            <ProtectedRoute>
                                <p>Profil-Seite</p>
                            </ProtectedRoute>
                        }
                    />
                </Routes>
            </MemoryRouter>
        </AuthProvider>
    )
}

/** Wartet, bis die Sitzungsprüfung beim Start abgeschlossen ist. */
async function renderAnonymousLoginPage(initialEntry?: string) {
    mockedFetchCurrentSession.mockResolvedValueOnce(null)

    const view = renderLoginPage(initialEntry)

    await screen.findByRole('button', {name: 'Anmelden'})

    return view
}

function emailField() {
    return screen.getByRole('textbox', {name: 'E-Mail-Adresse'})
}

function passwordField() {
    return screen.getByLabelText('Passwort')
}

async function fillAndSubmit(
    user: ReturnType<typeof userEvent.setup>,
    email = 'max@example.de',
    password = PASSWORD
) {
    await user.type(emailField(), email)
    await user.type(passwordField(), password)
    await user.click(screen.getByRole('button', {name: 'Anmelden'}))
}

describe('LoginPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(clearApolloCache).mockResolvedValue(undefined)
        localStorage.clear()
        sessionStorage.clear()
    })

    describe('Formular', () => {
        it('rendert Felder mit sichtbaren, zugeordneten Beschriftungen', async () => {
            await renderAnonymousLoginPage()

            expect(emailField()).toBeInTheDocument()
            expect(passwordField()).toBeInTheDocument()
            expect(
                screen.getByRole('heading', {name: 'Anmelden'})
            ).toBeInTheDocument()
        })

        it('verwendet passende Eingabetypen und Autocomplete-Werte', async () => {
            await renderAnonymousLoginPage()

            expect(emailField()).toHaveAttribute('type', 'email')
            expect(emailField()).toHaveAttribute('autocomplete', 'email')
            // type="password" verdeckt die Eingabe.
            expect(passwordField()).toHaveAttribute('type', 'password')
            expect(passwordField()).toHaveAttribute(
                'autocomplete',
                'current-password'
            )
        })

        it('verlinkt auf die Registrierung', async () => {
            await renderAnonymousLoginPage()

            expect(
                screen.getByRole('link', {name: 'Registrieren'})
            ).toHaveAttribute('href', '/register')
        })
    })

    it(
        'verlinkt auf die Passwort-Rücksetzung',
        async () => {
            await renderAnonymousLoginPage()

            expect(
                screen.getByRole('link', {
                    name: 'Passwort vergessen?'
                })
            ).toHaveAttribute(
                'href',
                '/forgot-password'
            )
        }
    )

    describe('clientseitige Validierung', () => {
        it('meldet leere Pflichtfelder, ohne die API aufzurufen', async () => {
            await renderAnonymousLoginPage()
            const user = userEvent.setup()

            await user.click(screen.getByRole('button', {name: 'Anmelden'}))

            expect(
                screen.getByText('Bitte gib deine E-Mail-Adresse ein.')
            ).toBeInTheDocument()
            expect(
                screen.getByText('Bitte gib dein Passwort ein.')
            ).toBeInTheDocument()
            expect(mockedCreateSession).not.toHaveBeenCalled()
        })

        it('meldet eine formal ungültige E-Mail-Adresse', async () => {
            await renderAnonymousLoginPage()
            const user = userEvent.setup()

            await fillAndSubmit(user, 'keine-mail')

            expect(
                screen.getByText('Bitte gib eine gültige E-Mail-Adresse ein.')
            ).toBeInTheDocument()
            expect(mockedCreateSession).not.toHaveBeenCalled()
        })

        it('ordnet Fehler per aria-invalid und aria-describedby zu', async () => {
            await renderAnonymousLoginPage()
            const user = userEvent.setup()

            await user.click(screen.getByRole('button', {name: 'Anmelden'}))

            expect(emailField()).toHaveAttribute('aria-invalid', 'true')
            expect(emailField()).toHaveAttribute(
                'aria-describedby',
                'login-email-error'
            )
            expect(passwordField()).toHaveAttribute(
                'aria-describedby',
                'login-password-error'
            )
            expect(
                screen.getByText('Bitte gib deine E-Mail-Adresse ein.')
            ).toHaveAttribute('id', 'login-email-error')
        })

        it('setzt den Fokus auf das erste fehlerhafte Feld', async () => {
            await renderAnonymousLoginPage()
            const user = userEvent.setup()

            await user.click(screen.getByRole('button', {name: 'Anmelden'}))

            expect(emailField()).toHaveFocus()
        })

        it('entfernt den Feldfehler nach einer Korrektur', async () => {
            await renderAnonymousLoginPage()
            const user = userEvent.setup()

            await user.click(screen.getByRole('button', {name: 'Anmelden'}))
            await user.type(emailField(), 'm')

            expect(
                screen.queryByText('Bitte gib deine E-Mail-Adresse ein.')
            ).not.toBeInTheDocument()
            expect(emailField()).toHaveAttribute('aria-invalid', 'false')
        })
    })

    describe('Anmeldung', () => {
        it('ruft bei gültigen Eingaben die Anmeldung mit getrimmter E-Mail auf', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockResolvedValueOnce({
                userId: session.userId,
                expiresAt: session.expiresAt
            })
            mockedFetchCurrentSession.mockResolvedValueOnce(session)
            const user = userEvent.setup()

            await fillAndSubmit(user, '  max@example.de  ')

            await waitFor(() =>
                expect(mockedCreateSession).toHaveBeenCalledWith({
                    email: 'max@example.de',
                    password: PASSWORD
                })
            )
        })

        it('leitet nach erfolgreicher Anmeldung zum Standardziel weiter', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockResolvedValueOnce({
                userId: session.userId,
                expiresAt: session.expiresAt
            })
            mockedFetchCurrentSession.mockResolvedValueOnce(session)
            const user = userEvent.setup()

            await fillAndSubmit(user)

            expect(
                await screen.findByText('Routenplaner-Seite')
            ).toBeInTheDocument()
        })

        it('leitet zur ursprünglich angefragten geschützten Seite weiter', async () => {
            mockedFetchCurrentSession.mockResolvedValueOnce(null)
            renderLoginPage('/profile')
            const user = userEvent.setup()

            await screen.findByRole('button', {name: 'Anmelden'})

            mockedCreateSession.mockResolvedValueOnce({
                userId: session.userId,
                expiresAt: session.expiresAt
            })
            mockedFetchCurrentSession.mockResolvedValueOnce(session)

            await fillAndSubmit(user)

            expect(await screen.findByText('Profil-Seite')).toBeInTheDocument()
        })

        it('leitet bereits angemeldete Benutzer von der Anmeldeseite weg', async () => {
            mockedFetchCurrentSession.mockResolvedValueOnce(session)

            renderLoginPage()

            expect(
                await screen.findByText('Routenplaner-Seite')
            ).toBeInTheDocument()
        })

        it('zeigt einen Ladezustand und verhindert mehrfaches Absenden', async () => {
            await renderAnonymousLoginPage()
            let finishLogin!: (value: {
                userId: string
                expiresAt: string
            }) => void
            mockedCreateSession.mockReturnValueOnce(
                new Promise((resolve) => {
                    finishLogin = resolve
                })
            )
            mockedFetchCurrentSession.mockResolvedValueOnce(session)
            const user = userEvent.setup()

            await user.type(emailField(), 'max@example.de')
            await user.type(passwordField(), PASSWORD)
            await user.type(passwordField(), '{Enter}')
            await user.type(passwordField(), '{Enter}')

            const busyButton = screen.getByRole('button', {
                name: 'Anmeldung läuft …'
            })
            expect(busyButton).toBeDisabled()
            expect(mockedCreateSession).toHaveBeenCalledTimes(1)

            finishLogin({
                userId: session.userId,
                expiresAt: session.expiresAt
            })

            expect(
                await screen.findByText('Routenplaner-Seite')
            ).toBeInTheDocument()
            expect(mockedCreateSession).toHaveBeenCalledTimes(1)
        })

        it('ist per Tastatur bedienbar', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockResolvedValueOnce({
                userId: session.userId,
                expiresAt: session.expiresAt
            })
            mockedFetchCurrentSession.mockResolvedValueOnce(session)
            const user = userEvent.setup()

            await user.tab()
            expect(emailField()).toHaveFocus()
            await user.keyboard('max@example.de')

            await user.tab()
            expect(passwordField()).toHaveFocus()
            await user.keyboard(`${PASSWORD}{Enter}`)

            await waitFor(() =>
                expect(mockedCreateSession).toHaveBeenCalledTimes(1)
            )
        })
    })

    describe('Fehlerbehandlung', () => {
        it('zeigt bei INVALID_CREDENTIALS einen allgemeinen Fehler', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockRejectedValueOnce(
                new ApiRequestError(401, {
                    code: 'INVALID_CREDENTIALS',
                    detail: 'Kein Konto für max@example.de'
                })
            )
            const user = userEvent.setup()

            await fillAndSubmit(user)

            const alert = await screen.findByRole('alert')
            expect(alert).toHaveTextContent(
                'Die E-Mail-Adresse oder das Passwort ist nicht korrekt.'
            )
            // Keine Zuordnung zu einem einzelnen Feld.
            expect(emailField()).toHaveAttribute('aria-invalid', 'false')
            expect(passwordField()).toHaveAttribute('aria-invalid', 'false')
            // Keine Hinweise darauf, ob die Adresse registriert ist.
            expect(alert).not.toHaveTextContent('Kein Konto')
        })

        it('leert nach einem Fehlversuch das Passwort und fokussiert es', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockRejectedValueOnce(
                new ApiRequestError(401, {code: 'INVALID_CREDENTIALS'})
            )
            const user = userEvent.setup()

            await fillAndSubmit(user)
            await screen.findByRole('alert')

            expect(emailField()).toHaveValue('max@example.de')
            expect(passwordField()).toHaveValue('')
            expect(passwordField()).toHaveFocus()
        })

        it('behandelt 429 mit Retry-After und sperrt das Absenden zeitweise', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockRejectedValueOnce(
                new ApiRequestError(
                    429,
                    {code: 'TOO_MANY_LOGIN_ATTEMPTS'},
                    60
                )
            )
            const user = userEvent.setup()

            await fillAndSubmit(user)

            expect(await screen.findByRole('alert')).toHaveTextContent(
                'Es wurden zu viele Anmeldeversuche durchgeführt. Bitte versuche es in 1 Minute erneut.'
            )
            expect(
                screen.getByRole('button', {name: /Erneut möglich in/})
            ).toBeDisabled()
        })

        it('behandelt 429 ohne Retry-After mit allgemeiner Meldung', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockRejectedValueOnce(
                new ApiRequestError(429, {})
            )
            const user = userEvent.setup()

            await fillAndSubmit(user)

            expect(await screen.findByRole('alert')).toHaveTextContent(
                'Es wurden zu viele Anmeldeversuche durchgeführt. Bitte versuche es später erneut.'
            )
            expect(
                screen.getByRole('button', {name: 'Anmelden'})
            ).toBeEnabled()
        })

        it.each([
            ['Serverfehler', new ApiRequestError(500, {})],
            ['Netzwerkfehler', new TypeError('Failed to fetch')]
        ])('zeigt bei %s eine technische Fehlermeldung', async (_, error) => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockRejectedValueOnce(error)
            const user = userEvent.setup()

            await fillAndSubmit(user)

            expect(await screen.findByRole('alert')).toHaveTextContent(
                'Die Anmeldung konnte nicht abgeschlossen werden. Bitte versuche es erneut.'
            )
        })

        it('zeigt eine technische Fehlermeldung, wenn die Sitzung nicht bestätigt wird', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockResolvedValueOnce({
                userId: session.userId,
                expiresAt: session.expiresAt
            })
            mockedFetchCurrentSession.mockResolvedValueOnce(null)
            const user = userEvent.setup()

            await fillAndSubmit(user)

            expect(await screen.findByRole('alert')).toHaveTextContent(
                'Die Anmeldung konnte nicht abgeschlossen werden.'
            )
            expect(
                screen.queryByText('Routenplaner-Seite')
            ).not.toBeInTheDocument()
        })

        it('erlaubt nach einem Fehler einen erneuten Versuch', async () => {
            await renderAnonymousLoginPage()
            mockedCreateSession.mockRejectedValueOnce(
                new ApiRequestError(401, {code: 'INVALID_CREDENTIALS'})
            )
            const user = userEvent.setup()

            await fillAndSubmit(user)
            await screen.findByRole('alert')

            mockedCreateSession.mockResolvedValueOnce({
                userId: session.userId,
                expiresAt: session.expiresAt
            })
            mockedFetchCurrentSession.mockResolvedValueOnce(session)

            await user.type(passwordField(), PASSWORD)
            await user.click(screen.getByRole('button', {name: 'Anmelden'}))

            expect(
                await screen.findByText('Routenplaner-Seite')
            ).toBeInTheDocument()
        })
    })

    describe('Datenschutz', () => {
        it('legt das Passwort nicht in Web Storage ab und liest keine Cookies', async () => {
            const cookieGetter = vi.spyOn(
                Document.prototype,
                'cookie',
                'get'
            )
            await renderAnonymousLoginPage()
            mockedCreateSession.mockResolvedValueOnce({
                userId: session.userId,
                expiresAt: session.expiresAt
            })
            mockedFetchCurrentSession.mockResolvedValueOnce(session)
            const user = userEvent.setup()

            await fillAndSubmit(user)
            await screen.findByText('Routenplaner-Seite')

            expect(localStorage).toHaveLength(0)
            expect(sessionStorage).toHaveLength(0)
            expect(cookieGetter).not.toHaveBeenCalled()
            cookieGetter.mockRestore()
        })
    })
})