// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {ApiRequestError} from '../../api/restClient'
import {
    mapChangeEmailError,
    mapChangePasswordError
} from './profileErrorMapping'

describe('mapChangeEmailError', () => {
    it('ordnet ein falsches Passwort dem Passwortfeld zu', () => {
        const result = mapChangeEmailError(
            new ApiRequestError(403, {code: 'INVALID_PASSWORD'})
        )

        expect(result.fieldErrors.password).toBe(
            'Das Passwort ist nicht korrekt.'
        )
    })

    it('ordnet eine vergebene Adresse dem E-Mail-Feld zu', () => {
        const result = mapChangeEmailError(
            new ApiRequestError(409, {code: 'EMAIL_ALREADY_EXISTS'})
        )

        expect(result.fieldErrors.newEmail).toContain('bereits ein Benutzerkonto')
    })

    it('meldet eine abgelaufene Sitzung', () => {
        const result = mapChangeEmailError(new ApiRequestError(401, {}))

        expect(result.formError).toContain('Sitzung')
    })

    it('zeigt bei unbekannten Fehlern eine allgemeine Meldung', () => {
        const result = mapChangeEmailError(new ApiRequestError(500, {}))

        expect(result.formError).toContain('konnte nicht geändert werden')
    })
})

describe('mapChangePasswordError', () => {
    it('ordnet ein falsches Passwort dem Feld "aktuelles Passwort" zu', () => {
        const result = mapChangePasswordError(
            new ApiRequestError(403, {code: 'INVALID_PASSWORD'})
        )

        expect(result.fieldErrors.currentPassword).toBe(
            'Das aktuelle Passwort ist nicht korrekt.'
        )
    })

    it('ordnet zu kurze Passwörter dem Feld "neues Passwort" zu', () => {
        const result = mapChangePasswordError(
            new ApiRequestError(422, {code: 'PASSWORD_REQUIREMENTS_NOT_MET'})
        )

        expect(result.fieldErrors.newPassword).toContain('zwölf Zeichen')
    })

    it('übernimmt Feldfehler der API', () => {
        const result = mapChangePasswordError(
            new ApiRequestError(422, {
                code: 'INVALID_ACCOUNT_CHANGE_REQUEST',
                fieldErrors: [
                    {
                        field: 'newPassword',
                        code: 'PASSWORD_UNCHANGED',
                        message: 'Bitte ein anderes Passwort wählen.'
                    }
                ]
            })
        )

        expect(result.fieldErrors.newPassword).toBe(
            'Bitte ein anderes Passwort wählen.'
        )
    })
})