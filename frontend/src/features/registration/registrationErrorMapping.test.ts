// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
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

    it('ordnet Top-Level-INVALID_PASSWORD dem Passwortfeld zu', () => {
        expect(
            mapRegistrationApiErrors(
                422,
                'INVALID_PASSWORD',
                [],
                'Das Passwort ist ungültig.'
            )
        ).toEqual({
            fieldErrors: {
                password:
                    'Das Passwort erfüllt die Anforderungen nicht.'
            }
        })
    })

    it('zeigt unbekannte fieldErrors zusätzlich allgemein an', () => {
        expect(
            mapRegistrationApiErrors(
                422,
                'INVALID_REGISTRATION_REQUEST',
                [
                    {
                        field: 'email',
                        code: 'INVALID_EMAIL',
                        message: 'Die E-Mail-Adresse ist ungültig.'
                    },
                    {
                        field: 'unknownField',
                        code: 'INVALID_FIELD',
                        message: 'Unbekannter Feldfehler.'
                    }
                ],
                'Die Registrierungsdaten sind ungültig.'
            )
        ).toEqual({
            fieldErrors: {
                email: 'Die E-Mail-Adresse ist ungültig.'
            },
            formError: 'Die Registrierungsdaten sind ungültig.'
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