// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {
    render,
    screen,
    waitFor
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter} from 'react-router'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi
} from 'vitest'
import {confirmPasswordReset} from '../api/passwordResetApi'
import {ApiRequestError} from '../api/restClient'
import ResetPasswordPage from './ResetPasswordPage'

vi.mock('../api/passwordResetApi', () => ({
    confirmPasswordReset: vi.fn()
}))

const mockedConfirmPasswordReset =
    vi.mocked(confirmPasswordReset)

const VALID_TOKEN = 'gueltiges-reset-token'
const VALID_PASSWORD = 'MeinNeuesPasswort123'

function renderPage(
    initialEntry =
    `/reset-password?token=${VALID_TOKEN}`
) {
    return render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <ResetPasswordPage />
        </MemoryRouter>
    )
}

function newPasswordField() {
    return screen.getByLabelText(
        'Neues Passwort'
    )
}

function passwordConfirmationField() {
    return screen.getByLabelText(
        'Neues Passwort wiederholen'
    )
}

function submitButton() {
    return screen.getByRole('button', {
        name: 'Neues Passwort speichern'
    })
}

async function fillPasswords(
    user: ReturnType<typeof userEvent.setup>,
    newPassword = VALID_PASSWORD,
    passwordConfirmation = VALID_PASSWORD
): Promise<void> {
    await user.type(
        newPasswordField(),
        newPassword
    )

    await user.type(
        passwordConfirmationField(),
        passwordConfirmation
    )
}

describe('ResetPasswordPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it(
        'zeigt einen Hinweis, wenn kein Token '
        + 'übergeben wurde',
        () => {
            renderPage('/reset-password')

            expect(
                screen.getByRole('heading', {
                    name: 'Rücksetzungslink unvollständig'
                })
            ).toBeInTheDocument()

            expect(
                screen.getByRole('alert')
            ).toHaveTextContent(
                'Es wurde kein Rücksetzungstoken '
                + 'übergeben.'
            )

            expect(
                screen.getByRole('link', {
                    name:
                        'Neuen Rücksetzungslink anfordern'
                })
            ).toHaveAttribute(
                'href',
                '/forgot-password'
            )

            expect(
                screen.getByRole('link', {
                    name: 'Zurück zum Login'
                })
            ).toHaveAttribute(
                'href',
                '/login'
            )

            expect(
                mockedConfirmPasswordReset
            ).not.toHaveBeenCalled()
        }
    )

    it(
        'zeigt das Formular für einen vorhandenen Token',
        () => {
            renderPage()

            expect(
                screen.getByRole('heading', {
                    name: 'Neues Passwort vergeben'
                })
            ).toBeInTheDocument()

            expect(
                newPasswordField()
            ).toHaveAttribute(
                'type',
                'password'
            )

            expect(
                newPasswordField()
            ).toHaveAttribute(
                'autocomplete',
                'new-password'
            )

            expect(
                passwordConfirmationField()
            ).toHaveAttribute(
                'type',
                'password'
            )

            expect(
                passwordConfirmationField()
            ).toHaveAttribute(
                'autocomplete',
                'new-password'
            )
        }
    )

    it(
        'meldet leere Passwortfelder, '
        + 'ohne die API aufzurufen',
        async () => {
            const user = userEvent.setup()
            renderPage()

            await user.click(submitButton())

            expect(
                screen.getByText(
                    'Bitte gib ein neues Passwort ein.'
                )
            ).toBeInTheDocument()

            expect(
                screen.getByText(
                    'Bitte wiederhole das neue Passwort.'
                )
            ).toBeInTheDocument()

            expect(
                newPasswordField()
            ).toHaveFocus()

            expect(
                mockedConfirmPasswordReset
            ).not.toHaveBeenCalled()
        }
    )

    it(
        'meldet ein zu kurzes Passwort',
        async () => {
            const user = userEvent.setup()
            renderPage()

            await fillPasswords(
                user,
                'zu-kurz',
                'zu-kurz'
            )

            await user.click(submitButton())

            expect(
                screen.getByText(
                    'Das Passwort muss mindestens '
                    + 'zwölf Zeichen lang sein.'
                )
            ).toBeInTheDocument()

            expect(
                mockedConfirmPasswordReset
            ).not.toHaveBeenCalled()
        }
    )

    it(
        'meldet nicht übereinstimmende Passwörter',
        async () => {
            const user = userEvent.setup()
            renderPage()

            await fillPasswords(
                user,
                VALID_PASSWORD,
                'EinAnderesPasswort123'
            )

            await user.click(submitButton())

            expect(
                screen.getByText(
                    'Die eingegebenen Passwörter '
                    + 'stimmen nicht überein.'
                )
            ).toBeInTheDocument()

            expect(
                passwordConfirmationField()
            ).toHaveFocus()

            expect(
                mockedConfirmPasswordReset
            ).not.toHaveBeenCalled()
        }
    )

    it(
        'sendet Token und neues Passwort '
        + 'an die API',
        async () => {
            const user = userEvent.setup()

            mockedConfirmPasswordReset
                .mockResolvedValueOnce(undefined)

            renderPage()

            await fillPasswords(user)
            await user.click(submitButton())

            await waitFor(() => {
                expect(
                    mockedConfirmPasswordReset
                ).toHaveBeenCalledWith(
                    VALID_TOKEN,
                    VALID_PASSWORD
                )
            })

            expect(
                mockedConfirmPasswordReset
            ).toHaveBeenCalledTimes(1)
        }
    )

    it(
        'zeigt nach erfolgreicher Rücksetzung '
        + 'eine Erfolgsmeldung und den Login-Link',
        async () => {
            const user = userEvent.setup()

            mockedConfirmPasswordReset
                .mockResolvedValueOnce(undefined)

            renderPage()

            await fillPasswords(user)
            await user.click(submitButton())

            expect(
                await screen.findByRole('heading', {
                    name:
                        'Passwort erfolgreich geändert'
                })
            ).toBeInTheDocument()

            expect(
                screen.getByRole('status')
            ).toHaveTextContent(
                'Dein neues Passwort wurde gespeichert.'
            )

            expect(
                screen.getByRole('link', {
                    name: 'Zum Login'
                })
            ).toHaveAttribute(
                'href',
                '/login'
            )
        }
    )

    it(
        'zeigt bei einem ungültigen Token '
        + 'eine verständliche Fehlermeldung',
        async () => {
            const user = userEvent.setup()

            mockedConfirmPasswordReset
                .mockRejectedValueOnce(
                    new ApiRequestError(
                        400,
                        {
                            code:
                                'INVALID_RESET_TOKEN',
                            detail:
                                'Interne Meldung'
                        }
                    )
                )

            renderPage(
                '/reset-password?token=abgelaufenes-token'
            )

            await fillPasswords(user)
            await user.click(submitButton())

            expect(
                await screen.findByRole('alert')
            ).toHaveTextContent(
                'Der Passwort-Rücksetzungslink ist '
                + 'ungültig, abgelaufen oder wurde '
                + 'bereits verwendet.'
            )

            expect(
                newPasswordField()
            ).toHaveValue('')

            expect(
                passwordConfirmationField()
            ).toHaveValue('')

            expect(
                newPasswordField()
            ).toHaveFocus()
        }
    )

    it(
        'zeigt bei nicht erfüllten Passwortanforderungen '
        + 'eine verständliche Meldung',
        async () => {
            const user = userEvent.setup()

            mockedConfirmPasswordReset
                .mockRejectedValueOnce(
                    new ApiRequestError(
                        422,
                        {
                            code:
                                'PASSWORD_REQUIREMENTS_NOT_MET',
                            detail:
                                'Interne Meldung'
                        }
                    )
                )

            renderPage()

            await fillPasswords(user)
            await user.click(submitButton())

            expect(
                await screen.findByRole('alert')
            ).toHaveTextContent(
                'Das Passwort muss mindestens '
                + 'zwölf Zeichen lang sein.'
            )

            expect(
                newPasswordField()
            ).toHaveValue('')

            expect(
                passwordConfirmationField()
            ).toHaveValue('')
        }
    )

    it(
        'zeigt bei Rate Limiting eine '
        + 'verständliche Fehlermeldung',
        async () => {
            const user = userEvent.setup()

            mockedConfirmPasswordReset
                .mockRejectedValueOnce(
                    new ApiRequestError(
                        429,
                        {
                            code:
                                'TOO_MANY_REQUESTS',
                            detail:
                                'Interne Meldung'
                        }
                    )
                )

            renderPage()

            await fillPasswords(user)
            await user.click(submitButton())

            expect(
                await screen.findByRole('alert')
            ).toHaveTextContent(
                'Zu viele Anfragen. Bitte versuche '
                + 'es später erneut.'
            )
        }
    )

    it(
        'zeigt bei einem Netzwerkfehler '
        + 'eine technische Fehlermeldung',
        async () => {
            const user = userEvent.setup()

            mockedConfirmPasswordReset
                .mockRejectedValueOnce(
                    new TypeError('Failed to fetch')
                )

            renderPage()

            await fillPasswords(user)
            await user.click(submitButton())

            expect(
                await screen.findByRole('alert')
            ).toHaveTextContent(
                'Das Passwort konnte nicht '
                + 'zurückgesetzt werden. '
                + 'Bitte versuche es erneut.'
            )

            expect(
                newPasswordField()
            ).toHaveFocus()
        }
    )

    it(
        'verhindert mehrfaches Absenden '
        + 'während einer laufenden Anfrage',
        async () => {
            const user = userEvent.setup()

            let finishRequest!: () => void

            mockedConfirmPasswordReset
                .mockReturnValueOnce(
                    new Promise<void>((resolve) => {
                        finishRequest = resolve
                    })
                )

            renderPage()

            await fillPasswords(user)
            await user.click(submitButton())

            const busyButton =
                screen.getByRole('button', {
                    name:
                        'Passwort wird gespeichert …'
                })

            expect(busyButton).toBeDisabled()

            await user.click(busyButton)

            expect(
                mockedConfirmPasswordReset
            ).toHaveBeenCalledTimes(1)

            finishRequest()

            expect(
                await screen.findByRole('heading', {
                    name:
                        'Passwort erfolgreich geändert'
                })
            ).toBeInTheDocument()
        }
    )
})