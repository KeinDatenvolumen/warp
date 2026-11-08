// [KI-Log Nr. KI-092] (474d299) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend registration tests (WARP-005)
// [KI-Log Nr. KI-113] (df21fe0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend registration tests (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {
    validateRegistration,
    type RegistrationValues
} from './registrationValidation'

const validValues: RegistrationValues = {
    name: 'Max Mustermann',
    email: 'max@example.de',
    password: 'MeinSicheresPasswort123'
}

describe('validateRegistration', () => {
    it('akzeptiert vollständige gültige Registrierungsdaten', () => {
        expect(validateRegistration(validValues)).toEqual({})
    })

    it('meldet einen fehlenden Namen', () => {
        expect(
            validateRegistration({
                ...validValues,
                name: ''
            })
        ).toEqual({
            name: 'Bitte gib deinen Namen ein.'
        })
    })

    it('meldet eine fehlende E-Mail-Adresse', () => {
        expect(
            validateRegistration({
                ...validValues,
                email: ''
            })
        ).toEqual({
            email: 'Bitte gib deine E-Mail-Adresse ein.'
        })
    })

    it('meldet eine ungültige E-Mail-Adresse', () => {
        expect(
            validateRegistration({
                ...validValues,
                email: 'ungueltige-email'
            })
        ).toEqual({
            email: 'Bitte gib eine gültige E-Mail-Adresse ein.'
        })
    })

    it('meldet ein zu kurzes Passwort', () => {
        expect(
            validateRegistration({
                ...validValues,
                password: '12345678901'
            })
        ).toEqual({
            password:
                'Das Passwort muss mindestens zwölf Zeichen lang sein.'
        })
    })

    it('akzeptiert ein Passwort mit genau zwölf Zeichen', () => {
        expect(
            validateRegistration({
                ...validValues,
                password: '123456789012'
            })
        ).toEqual({})
    })
})