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
import {sendPasswordResetEmail} from '../api/passwordResetApi'
import {ApiRequestError} from '../api/restClient'
import ForgotPasswordPage from './ForgotPasswordPage'

vi.mock('../api/passwordResetApi', () => ({
    sendPasswordResetEmail: vi.fn()
}))

const mockedSendPasswordResetEmail =
    vi.mocked(sendPasswordResetEmail)

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/forgot-password']}>
            <ForgotPasswordPage />
        </MemoryRouter>
    )
}

function emailField() {
    return screen.getByRole('textbox', {
        name: 'E-Mail-Adresse'
    })
}

function submitButton() {
    return screen.getByRole('button', {
        name: 'Rücksetzungslink anfordern'
    })
}

describe('ForgotPasswordPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('zeigt das Formular und den Link zum Login', () => {
        renderPage()

        expect(
            screen.getByRole('heading', {
                name: 'Passwort vergessen?'
            })
        ).toBeInTheDocument()

        expect(emailField()).toBeInTheDocument()

        expect(emailField()).toHaveAttribute(
            'type',
            'email'
        )

        expect(emailField()).toHaveAttribute(
            'autocomplete',
            'email'
        )

        expect(
            screen.getByRole('link', {
                name: 'Zurück zum Login'
            })
        ).toHaveAttribute(
            'href',
            '/login'
        )
    })

    it(
        'meldet eine fehlende E-Mail-Adresse, '
        + 'ohne die API aufzurufen',
        async () => {
            const user = userEvent.setup()
            renderPage()

            await user.click(submitButton())

            expect(
                screen.getByText(
                    'Bitte gib deine E-Mail-Adresse ein.'
                )
            ).toBeInTheDocument()

            expect(emailField()).toHaveFocus()

            expect(
                mockedSendPasswordResetEmail
            ).not.toHaveBeenCalled()
        }
    )

    it(
        'meldet eine ungültige E-Mail-Adresse, '
        + 'ohne die API aufzurufen',
        async () => {
            const user = userEvent.setup()
            renderPage()

            await user.type(
                emailField(),
                'keine-gueltige-email'
            )

            await user.click(submitButton())

            expect(
                screen.getByText(
                    'Bitte gib eine gültige '
                    + 'E-Mail-Adresse ein.'
                )
            ).toBeInTheDocument()

            expect(
                mockedSendPasswordResetEmail
            ).not.toHaveBeenCalled()
        }
    )

    it(
        'sendet eine getrimmte E-Mail-Adresse '
        + 'an die API',
        async () => {
            const user = userEvent.setup()

            mockedSendPasswordResetEmail
                .mockResolvedValueOnce(undefined)

            renderPage()

            await user.type(
                emailField(),
                '  max@example.de  '
            )

            await user.click(submitButton())

            await waitFor(() => {
                expect(
                    mockedSendPasswordResetEmail
                ).toHaveBeenCalledWith(
                    'max@example.de'
                )
            })

            expect(
                mockedSendPasswordResetEmail
            ).toHaveBeenCalledTimes(1)
        }
    )

    it(
        'zeigt nach erfolgreicher Anfrage '
        + 'eine neutrale Erfolgsmeldung',
        async () => {
            const user = userEvent.setup()

            mockedSendPasswordResetEmail
                .mockResolvedValueOnce(undefined)

            renderPage()

            await user.type(
                emailField(),
                'max@example.de'
            )

            await user.click(submitButton())

            expect(
                await screen.findByRole('status')
            ).toHaveTextContent(
                'Falls für diese E-Mail-Adresse ein '
                + 'Benutzerkonto existiert, wurde ein '
                + 'Link zum Zurücksetzen des Passworts '
                + 'versendet.'
            )

            expect(emailField()).toHaveValue('')
        }
    )

    it(
        'verrät in der Erfolgsmeldung nicht, '
        + 'ob das Benutzerkonto existiert',
        async () => {
            const user = userEvent.setup()

            mockedSendPasswordResetEmail
                .mockResolvedValueOnce(undefined)

            renderPage()

            await user.type(
                emailField(),
                'unbekannt@example.de'
            )

            await user.click(submitButton())

            const status =
                await screen.findByRole('status')

            expect(status).toHaveTextContent(
                'Falls für diese E-Mail-Adresse '
                + 'ein Benutzerkonto existiert'
            )

            expect(status).not.toHaveTextContent(
                'Benutzerkonto wurde gefunden'
            )

            expect(status).not.toHaveTextContent(
                'Benutzerkonto existiert nicht'
            )
        }
    )

    it(
        'zeigt bei Rate Limiting eine '
        + 'verständliche Fehlermeldung',
        async () => {
            const user = userEvent.setup()

            mockedSendPasswordResetEmail
                .mockRejectedValueOnce(
                    new ApiRequestError(
                        429,
                        {
                            code: 'TOO_MANY_REQUESTS',
                            detail: 'Interne Meldung'
                        }
                    )
                )

            renderPage()

            await user.type(
                emailField(),
                'max@example.de'
            )

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

            mockedSendPasswordResetEmail
                .mockRejectedValueOnce(
                    new TypeError('Failed to fetch')
                )

            renderPage()

            await user.type(
                emailField(),
                'max@example.de'
            )

            await user.click(submitButton())

            expect(
                await screen.findByRole('alert')
            ).toHaveTextContent(
                'Die Passwort-Rücksetzungs-E-Mail '
                + 'konnte nicht versendet werden. '
                + 'Bitte versuche es später erneut.'
            )

            expect(emailField()).toHaveFocus()
        }
    )

    it(
        'verhindert mehrfaches Absenden '
        + 'während einer laufenden Anfrage',
        async () => {
            const user = userEvent.setup()

            let finishRequest!: () => void

            mockedSendPasswordResetEmail.mockReturnValueOnce(
                new Promise<void>((resolve) => {
                    finishRequest = resolve
                })
            )

            renderPage()

            await user.type(
                emailField(),
                'max@example.de'
            )

            await user.click(submitButton())

            const busyButton =
                screen.getByRole('button', {
                    name: 'Link wird versendet …'
                })

            expect(busyButton).toBeDisabled()

            await user.click(busyButton)

            expect(
                mockedSendPasswordResetEmail
            ).toHaveBeenCalledTimes(1)

            finishRequest()

            expect(
                await screen.findByRole('status')
            ).toBeInTheDocument()
        }
    )
})