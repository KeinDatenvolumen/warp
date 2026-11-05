// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
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