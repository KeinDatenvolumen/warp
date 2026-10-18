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
import {beforeEach, describe, expect, it, vi} from 'vitest'
import RegistrationPage from './RegistrationPage'
import {registerUser} from '../api/registrationApi'
import {ApiRequestError} from '../api/restClient'
import type {RegistrationResponse} from '../types/registration'

vi.mock('../api/registrationApi', () => ({
    registerUser: vi.fn()
}))

const mockedRegisterUser = vi.mocked(registerUser)

const validResponse: RegistrationResponse = {
    id: 'usr_123',
    name: 'Max Mustermann',
    email: 'max@example.de',
    emailVerified: false,
    createdAt: '2026-10-15T10:00:00Z',
    message:
        'Das Benutzerkonto wurde erfolgreich erstellt.'
}

function renderRegistrationPage() {
    return render(
        <MemoryRouter>
            <RegistrationPage />
        </MemoryRouter>
    )
}

async function fillValidForm() {
    renderRegistrationPage()

    const user = userEvent.setup()

    await user.type(
        screen.getByRole('textbox', {
            name: 'Name'
        }),
        'Max Mustermann'
    )

    await user.type(
        screen.getByRole('textbox', {
            name: 'E-Mail-Adresse'
        }),
        'max@example.de'
    )

    await user.type(
        screen.getByLabelText('Passwort'),
        'MeinSicheresPasswort123'
    )

    return user
}

describe('RegistrationPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('rendert die drei Registrierungsfelder mit sichtbaren Beschriftungen', () => {
        renderRegistrationPage()

        expect(
            screen.getByRole('textbox', {
                name: 'Name'
            })
        ).toBeInTheDocument()

        expect(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            })
        ).toBeInTheDocument()

        expect(
            screen.getByLabelText('Passwort')
        ).toBeInTheDocument()
    })

    it('verwendet die passenden HTML-Typen', () => {
        renderRegistrationPage()

        expect(
            screen.getByRole('textbox', {
                name: 'Name'
            })
        ).toHaveAttribute('type', 'text')

        expect(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            })
        ).toHaveAttribute('type', 'email')

        expect(
            screen.getByLabelText('Passwort')
        ).toHaveAttribute('type', 'password')
    })

    it('verwendet die passenden Autocomplete-Attribute', () => {
        renderRegistrationPage()

        expect(
            screen.getByRole('textbox', {
                name: 'Name'
            })
        ).toHaveAttribute('autocomplete', 'name')

        expect(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            })
        ).toHaveAttribute('autocomplete', 'email')

        expect(
            screen.getByLabelText('Passwort')
        ).toHaveAttribute(
            'autocomplete',
            'new-password'
        )
    })

    it('sendet leere Pflichtfelder nicht ab', async () => {
        const user = userEvent.setup()

        renderRegistrationPage()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        expect(mockedRegisterUser).not.toHaveBeenCalled()

        expect(
            screen.getByText('Bitte gib deinen Namen ein.')
        ).toBeInTheDocument()

        expect(
            screen.getByText(
                'Bitte gib deine E-Mail-Adresse ein.'
            )
        ).toBeInTheDocument()

        expect(
            screen.getByText('Bitte gib ein Passwort ein.')
        ).toBeInTheDocument()
    })

    it('weist eine formal ungültige E-Mail-Adresse zurück', async () => {
        const user = userEvent.setup()

        renderRegistrationPage()

        await user.type(
            screen.getByRole('textbox', {
                name: 'Name'
            }),
            'Max Mustermann'
        )

        await user.type(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            }),
            'ungueltige-email'
        )

        await user.type(
            screen.getByLabelText('Passwort'),
            'MeinSicheresPasswort123'
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        expect(mockedRegisterUser).not.toHaveBeenCalled()

        expect(
            screen.getByText(
                'Bitte gib eine gültige E-Mail-Adresse ein.'
            )
        ).toBeInTheDocument()
    })

    it('weist ein Passwort mit weniger als zwölf Zeichen zurück', async () => {
        const user = userEvent.setup()

        renderRegistrationPage()

        await user.type(
            screen.getByRole('textbox', {
                name: 'Name'
            }),
            'Max Mustermann'
        )

        await user.type(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            }),
            'max@example.de'
        )

        await user.type(
            screen.getByLabelText('Passwort'),
            '12345678901'
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        expect(mockedRegisterUser).not.toHaveBeenCalled()

        expect(
            screen.getByText(
                'Das Passwort muss mindestens zwölf Zeichen lang sein.'
            )
        ).toBeInTheDocument()
    })

    it('zeichnet ungültige Felder mit aria-invalid und aria-describedby aus', async () => {
        const user = userEvent.setup()

        renderRegistrationPage()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        const nameInput = screen.getByRole('textbox', {
            name: 'Name'
        })

        const emailInput = screen.getByRole('textbox', {
            name: 'E-Mail-Adresse'
        })

        const passwordInput = screen.getByLabelText('Passwort')

        expect(nameInput).toHaveAttribute(
            'aria-invalid',
            'true'
        )

        expect(nameInput).toHaveAttribute(
            'aria-describedby',
            'registration-name-error'
        )

        expect(emailInput).toHaveAttribute(
            'aria-invalid',
            'true'
        )

        expect(emailInput).toHaveAttribute(
            'aria-describedby',
            'registration-email-error'
        )

        expect(passwordInput).toHaveAttribute(
            'aria-invalid',
            'true'
        )

        expect(passwordInput).toHaveAttribute(
            'aria-describedby',
            'registration-password-error'
        )
    })

    it('entfernt den Feldfehler nach der Korrektur', async () => {
        const user = userEvent.setup()

        renderRegistrationPage()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        expect(
            screen.getByText('Bitte gib deinen Namen ein.')
        ).toBeInTheDocument()

        const nameInput = screen.getByRole('textbox', {
            name: 'Name'
        })

        await user.type(nameInput, 'Max Mustermann')

        expect(
            screen.queryByText('Bitte gib deinen Namen ein.')
        ).not.toBeInTheDocument()

        expect(nameInput).toHaveAttribute(
            'aria-invalid',
            'false'
        )

        expect(nameInput).not.toHaveAttribute(
            'aria-describedby'
        )
    })

    it('sendet gültige Daten an die Registrierungs-API', async () => {
        mockedRegisterUser.mockResolvedValue(validResponse)

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        await waitFor(() => {
            expect(mockedRegisterUser).toHaveBeenCalledWith({
                name: 'Max Mustermann',
                email: 'max@example.de',
                password: 'MeinSicheresPasswort123'
            })
        })

        expect(mockedRegisterUser).toHaveBeenCalledTimes(1)
    })

    it('entfernt Leerzeichen bei Name und E-Mail vor dem Absenden', async () => {
        mockedRegisterUser.mockResolvedValue(validResponse)

        const user = userEvent.setup()

        renderRegistrationPage()

        await user.type(
            screen.getByRole('textbox', {
                name: 'Name'
            }),
            '  Max Mustermann  '
        )

        await user.type(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            }),
            '  max@example.de  '
        )

        await user.type(
            screen.getByLabelText('Passwort'),
            'MeinSicheresPasswort123'
        )

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        await waitFor(() => {
            expect(mockedRegisterUser).toHaveBeenCalledWith({
                name: 'Max Mustermann',
                email: 'max@example.de',
                password: 'MeinSicheresPasswort123'
            })
        })
    })

    it('zeigt während der Anfrage den Ladezustand an', async () => {
        let resolveRequest!: (
            response: RegistrationResponse
        ) => void

        mockedRegisterUser.mockReturnValue(
            new Promise((resolve) => {
                resolveRequest = resolve
            })
        )

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        const loadingButton = screen.getByRole('button', {
            name: 'Registrierung läuft …'
        })

        expect(loadingButton).toBeDisabled()

        resolveRequest(validResponse)

        await waitFor(() => {
            expect(
                screen.getByRole('status')
            ).toHaveTextContent(
                'Das Benutzerkonto wurde erfolgreich erstellt.'
            )
        })

        expect(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        ).toBeEnabled()
    })

    it('verhindert mehrfaches Absenden während der Anfrage', async () => {
        let resolveRequest!: (
            response: RegistrationResponse
        ) => void

        mockedRegisterUser.mockReturnValue(
            new Promise((resolve) => {
                resolveRequest = resolve
            })
        )

        const user = await fillValidForm()

        const submitButton = screen.getByRole('button', {
            name: 'Registrieren'
        })

        await user.click(submitButton)

        expect(submitButton).toBeDisabled()

        await user.click(submitButton)

        expect(mockedRegisterUser).toHaveBeenCalledTimes(1)

        resolveRequest(validResponse)

        await waitFor(() => {
            expect(
                screen.getByRole('status')
            ).toBeInTheDocument()
        })
    })

    it('zeigt nach erfolgreicher Registrierung die Meldung des BFF an', async () => {
        mockedRegisterUser.mockResolvedValue(validResponse)

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        expect(
            await screen.findByRole('status')
        ).toHaveTextContent(
            'Das Benutzerkonto wurde erfolgreich erstellt.'
        )
    })

    it('ordnet Top-Level-INVALID_PASSWORD dem Passwortfeld zu', async () => {
        mockedRegisterUser.mockRejectedValue(
            new ApiRequestError(422, {
                code: 'INVALID_PASSWORD',
                detail: 'Das Passwort ist ungültig.'
            })
        )

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        const passwordInput =
            screen.getByLabelText('Passwort')

        expect(
            await screen.findByText(
                'Das Passwort erfüllt die Anforderungen nicht.'
            )
        ).toBeInTheDocument()

        expect(passwordInput).toHaveAttribute(
            'aria-invalid',
            'true'
        )
    })

    it('ordnet EMAIL_ALREADY_EXISTS dem E-Mail-Feld zu', async () => {
        mockedRegisterUser.mockRejectedValue(
            new ApiRequestError(409, {
                code: 'EMAIL_ALREADY_EXISTS',
                detail:
                    'Für diese E-Mail-Adresse existiert bereits ein Benutzerkonto.'
            })
        )

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        const emailInput = screen.getByRole('textbox', {
            name: 'E-Mail-Adresse'
        })

        expect(
            await screen.findByText(
                'Für diese E-Mail-Adresse ist bereits ein Benutzerkonto vorhanden.'
            )
        ).toBeInTheDocument()

        expect(emailInput).toHaveAttribute(
            'aria-invalid',
            'true'
        )

        expect(emailInput).toHaveAttribute(
            'aria-describedby',
            'registration-email-error'
        )
    })

    it('ordnet INVALID_PASSWORD dem Passwortfeld zu', async () => {
        mockedRegisterUser.mockRejectedValue(
            new ApiRequestError(422, {
                code: 'INVALID_REGISTRATION_REQUEST',
                detail:
                    'Die Registrierungsdaten sind ungültig.',
                fieldErrors: [
                    {
                        field: 'password',
                        code: 'INVALID_PASSWORD',
                        message:
                            'Das Passwort muss mindestens zwölf Zeichen lang sein.'
                    }
                ]
            })
        )

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        const passwordInput =
            screen.getByLabelText('Passwort')

        expect(
            await screen.findByText(
                'Das Passwort muss mindestens zwölf Zeichen lang sein.'
            )
        ).toBeInTheDocument()

        expect(passwordInput).toHaveAttribute(
            'aria-invalid',
            'true'
        )

        expect(passwordInput).toHaveAttribute(
            'aria-describedby',
            'registration-password-error'
        )
    })

    it('ordnet serverseitige E-Mail-fieldErrors dem E-Mail-Feld zu', async () => {
        mockedRegisterUser.mockRejectedValue(
            new ApiRequestError(422, {
                code: 'INVALID_REGISTRATION_REQUEST',
                detail:
                    'Die Registrierungsdaten sind ungültig.',
                fieldErrors: [
                    {
                        field: 'email',
                        code: 'INVALID_EMAIL',
                        message:
                            'Die E-Mail-Adresse ist ungültig.'
                    }
                ]
            })
        )

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        expect(
            await screen.findByText(
                'Die E-Mail-Adresse ist ungültig.'
            )
        ).toBeInTheDocument()

        expect(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            })
        ).toHaveAttribute('aria-invalid', 'true')
    })

    it('zeigt unerwartete technische Fehler allgemein an', async () => {
        mockedRegisterUser.mockRejectedValue(
            new Error('Network failure')
        )

        const user = await fillValidForm()

        await user.click(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )

        expect(
            await screen.findByRole('alert')
        ).toHaveTextContent(
            'Die Registrierung konnte nicht abgeschlossen werden. Bitte versuche es erneut.'
        )
    })

    it('ermöglicht die Bedienung des Formulars per Tastatur', async () => {
        const user = userEvent.setup()

        renderRegistrationPage()

        await user.tab()
        expect(document.activeElement).toBe(
            screen.getByRole('textbox', {
                name: 'Name'
            })
        )

        await user.tab()
        expect(document.activeElement).toBe(
            screen.getByRole('textbox', {
                name: 'E-Mail-Adresse'
            })
        )

        await user.tab()
        expect(document.activeElement).toBe(
            screen.getByLabelText('Passwort')
        )

        await user.tab()
        expect(document.activeElement).toBe(
            screen.getByRole('button', {
                name: 'Registrieren'
            })
        )
    })
})