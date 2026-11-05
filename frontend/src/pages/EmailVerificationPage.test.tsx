// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
/**
 * @vitest-environment jsdom
 */

import {StrictMode} from 'react'
import {render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter} from 'react-router'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import EmailVerificationPage from './EmailVerificationPage'
import {confirmEmail, resendVerificationEmail} from '../api/emailVerificationApi'
import {ApiRequestError} from '../api/restClient'

vi.mock('../api/emailVerificationApi', () => ({
    confirmEmail: vi.fn(),
    resendVerificationEmail: vi.fn()
}))

const mockedConfirmEmail = vi.mocked(confirmEmail)
const mockedResendVerificationEmail = vi.mocked(resendVerificationEmail)

function renderPage(initialEntries = ['/verify-email']) {
    return render(
        <StrictMode>
            <MemoryRouter initialEntries={initialEntries}>
                <EmailVerificationPage />
            </MemoryRouter>
        </StrictMode>
    )
}

describe('EmailVerificationPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('bestätigt ein gültiges Token und zeigt Erfolgsmeldung mit Login-Link', async () => {
        mockedConfirmEmail.mockResolvedValueOnce(undefined)

        renderPage(['/verify-email?token=valid-token-123'])

        expect(
            screen.getByRole('heading', {name: /E-Mail-Adresse wird bestätigt/i})
        ).toBeInTheDocument()

        await waitFor(() => {
            expect(mockedConfirmEmail).toHaveBeenCalledWith('valid-token-123')
        })

        expect(
            await screen.findByRole('heading', {name: 'E-Mail-Adresse bestätigt'})
        ).toBeInTheDocument()
        expect(
            screen.getByText(/Deine E-Mail-Adresse wurde erfolgreich bestätigt/i)
        ).toBeInTheDocument()

        const loginLink = screen.getByRole('link', {name: 'Zum Login'})
        expect(loginLink).toBeInTheDocument()
        expect(loginLink).toHaveAttribute('href', '/login')
    })

    it('zeigt Fehlermeldung und Resend-Formular bei ungültigem Token', async () => {
        mockedConfirmEmail.mockRejectedValueOnce(
            new ApiRequestError(400, {
                code: 'INVALID_VERIFICATION_TOKEN',
                detail: 'Der E-Mail-Verifikationslink ist ungültig oder abgelaufen'
            })
        )

        renderPage(['/verify-email?token=expired-token'])

        expect(
            await screen.findByRole('heading', {
                name: 'Bestätigung fehlgeschlagen'
            })
        ).toBeInTheDocument()

        expect(screen.getByRole('alert')).toHaveTextContent(
            'Der Bestätigungslink ist ungültig, abgelaufen oder wurde bereits verwendet.'
        )

        expect(
            screen.getByRole('heading', {
                name: 'Bestätigungs-E-Mail erneut anfordern'
            })
        ).toBeInTheDocument()
        expect(
            screen.getByRole('textbox', {name: 'E-Mail-Adresse'})
        ).toBeInTheDocument()
    })

    it('zeigt Hinweis und Resend-Formular, wenn kein Token übergeben wurde', () => {
        renderPage(['/verify-email'])

        expect(
            screen.getByRole('heading', {
                name: 'Bestätigungslink unvollständig'
            })
        ).toBeInTheDocument()

        expect(screen.getByRole('alert')).toHaveTextContent(
            'Es wurde kein Bestätigungstoken übergeben.'
        )
        expect(mockedConfirmEmail).not.toHaveBeenCalled()

        expect(
            screen.getByRole('heading', {
                name: 'Bestätigungs-E-Mail erneut anfordern'
            })
        ).toBeInTheDocument()
    })

    it('validiert die E-Mail-Eingabe beim erneuten Senden', async () => {
        const user = userEvent.setup()
        renderPage(['/verify-email'])

        const submitButton = screen.getByRole('button', {
            name: 'Neue Bestätigungs-E-Mail senden'
        })

        await user.click(submitButton)

        expect(
            await screen.findByText('Bitte gib deine E-Mail-Adresse ein.')
        ).toBeInTheDocument()
        expect(mockedResendVerificationEmail).not.toHaveBeenCalled()

        const emailInput = screen.getByRole('textbox', {name: 'E-Mail-Adresse'})
        await user.type(emailInput, 'ungueltig')
        await user.click(submitButton)

        expect(
            await screen.findByText('Bitte gib eine gültige E-Mail-Adresse ein.')
        ).toBeInTheDocument()
        expect(mockedResendVerificationEmail).not.toHaveBeenCalled()
    })

    it('sendet bei gültiger E-Mail eine neue Verifikations-Mail an und zeigt Bestätigung', async () => {
        const user = userEvent.setup()
        mockedResendVerificationEmail.mockResolvedValueOnce(undefined)

        renderPage(['/verify-email'])

        const emailInput = screen.getByRole('textbox', {name: 'E-Mail-Adresse'})
        await user.type(emailInput, '  max@example.de  ')

        const submitButton = screen.getByRole('button', {
            name: 'Neue Bestätigungs-E-Mail senden'
        })
        await user.click(submitButton)

        await waitFor(() => {
            expect(mockedResendVerificationEmail).toHaveBeenCalledWith(
                'max@example.de'
            )
        })

        expect(
            await screen.findByRole('status')
        ).toHaveTextContent(
            'Falls ein Benutzerkonto mit dieser E-Mail-Adresse existiert, wurde eine neue Bestätigungs-E-Mail versendet.'
        )
        expect(emailInput).toHaveValue('')
    })

    it('behandelt 429 Rate Limiting beim erneuten Senden', async () => {
        const user = userEvent.setup()
        mockedResendVerificationEmail.mockRejectedValueOnce(
            new ApiRequestError(429, {
                code: 'TOO_MANY_REQUESTS',
                detail: 'Zu viele Anfragen'
            })
        )

        renderPage(['/verify-email'])

        const emailInput = screen.getByRole('textbox', {name: 'E-Mail-Adresse'})
        await user.type(emailInput, 'max@example.de')

        await user.click(
            screen.getByRole('button', {
                name: 'Neue Bestätigungs-E-Mail senden'
            })
        )

        expect(
            await screen.findByText(
                'Zu viele Anfragen. Bitte versuche es später erneut.'
            )
        ).toBeInTheDocument()
    })
})
