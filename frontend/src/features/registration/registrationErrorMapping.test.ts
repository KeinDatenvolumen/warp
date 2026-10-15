import {describe, expect, it} from 'vitest'
import {mapRegistrationApiErrors} from './registrationErrorMapping'

describe('mapRegistrationApiErrors', () => {
    it('ordnet EMAIL_ALREADY_EXISTS dem E-Mail-Feld zu', () => {
        expect(
            mapRegistrationApiErrors(
                409,
                'EMAIL_ALREADY_EXISTS',
                [],
                'E-Mail bereits vorhanden'
            )
        ).toEqual({
            fieldErrors: {
                email:
                    'Für diese E-Mail-Adresse ist bereits ein Benutzerkonto vorhanden.'
            }
        })
    })

    it('ordnet INVALID_PASSWORD dem Passwortfeld zu', () => {
        expect(
            mapRegistrationApiErrors(
                422,
                'INVALID_REGISTRATION_REQUEST',
                [
                    {
                        field: 'password',
                        code: 'INVALID_PASSWORD',
                        message:
                            'Das Passwort muss mindestens zwölf Zeichen lang sein.'
                    }
                ],
                'Die Registrierungsdaten sind ungültig.'
            )
        ).toEqual({
            fieldErrors: {
                password:
                    'Das Passwort muss mindestens zwölf Zeichen lang sein.'
            }
        })
    })

    it('ordnet einen E-Mail-fieldError dem E-Mail-Feld zu', () => {
        expect(
            mapRegistrationApiErrors(
                422,
                'INVALID_REGISTRATION_REQUEST',
                [
                    {
                        field: 'email',
                        code: 'INVALID_EMAIL',
                        message:
                            'Die E-Mail-Adresse ist ungültig.'
                    }
                ],
                'Die Registrierungsdaten sind ungültig.'
            )
        ).toEqual({
            fieldErrors: {
                email:
                    'Die E-Mail-Adresse ist ungültig.'
            }
        })
    })

    it('ordnet unbekannte Fehler einer allgemeinen Formularmeldung zu', () => {
        expect(
            mapRegistrationApiErrors(
                500,
                'INTERNAL_SERVER_ERROR',
                [],
                'Technischer Fehler'
            )
        ).toEqual({
            fieldErrors: {},
            formError: 'Technischer Fehler'
        })
    })

    it('verwendet den Standardtext ohne Backend-Detailmeldung', () => {
        expect(
            mapRegistrationApiErrors(
                500,
                'INTERNAL_SERVER_ERROR',
                [],
                ''
            )
        ).toEqual({
            fieldErrors: {},
            formError:
                'Die Registrierung konnte nicht abgeschlossen werden. Bitte versuche es erneut.'
        })
    })
})