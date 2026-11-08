// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {
    validateChangeEmail,
    validateChangePassword
} from './profileValidation'

describe('validateChangeEmail', () => {
    const currentEmail = 'max@example.de'

    it('akzeptiert gültige Eingaben', () => {
        expect(
            validateChangeEmail(
                {newEmail: 'neu@example.de', password: 'geheim'},
                currentEmail
            )
        ).toEqual({})
    })

    it('verlangt E-Mail-Adresse und Passwort', () => {
        expect(
            validateChangeEmail({newEmail: '  ', password: ''}, currentEmail)
        ).toEqual({
            newEmail: 'Bitte gib die neue E-Mail-Adresse ein.',
            password: 'Bitte gib dein Passwort zur Bestätigung ein.'
        })
    })

    it('lehnt ungültige Adressen ab', () => {
        expect(
            validateChangeEmail(
                {newEmail: 'keine-mail', password: 'geheim'},
                currentEmail
            ).newEmail
        ).toBe('Bitte gib eine gültige E-Mail-Adresse ein.')
    })

    it('lehnt die aktuelle Adresse ab, auch bei anderer Schreibweise', () => {
        expect(
            validateChangeEmail(
                {newEmail: 'MAX@example.de', password: 'geheim'},
                currentEmail
            ).newEmail
        ).toBe(
            'Die neue E-Mail-Adresse muss sich von der aktuellen unterscheiden.'
        )
    })
})

describe('validateChangePassword', () => {
    const valid = {
        currentPassword: 'altes-passwort-123',
        newPassword: 'neues-passwort-123',
        passwordConfirmation: 'neues-passwort-123'
    }

    it('akzeptiert gültige Eingaben', () => {
        expect(validateChangePassword(valid)).toEqual({})
    })

    it('verlangt das aktuelle Passwort', () => {
        expect(
            validateChangePassword({...valid, currentPassword: ''})
                .currentPassword
        ).toBe('Bitte gib dein aktuelles Passwort ein.')
    })

    it('verlangt mindestens zwölf Zeichen', () => {
        expect(
            validateChangePassword({
                ...valid,
                newPassword: 'kurz',
                passwordConfirmation: 'kurz'
            }).newPassword
        ).toBe('Das Passwort muss mindestens zwölf Zeichen lang sein.')
    })

    it('lehnt ein unverändertes Passwort ab', () => {
        expect(
            validateChangePassword({
                ...valid,
                newPassword: valid.currentPassword,
                passwordConfirmation: valid.currentPassword
            }).newPassword
        ).toBe(
            'Das neue Passwort muss sich vom aktuellen Passwort unterscheiden.'
        )
    })

    it('erkennt abweichende Wiederholungen', () => {
        expect(
            validateChangePassword({
                ...valid,
                passwordConfirmation: 'etwas-anderes-123'
            }).passwordConfirmation
        ).toBe('Die eingegebenen Passwörter stimmen nicht überein.')
    })
})