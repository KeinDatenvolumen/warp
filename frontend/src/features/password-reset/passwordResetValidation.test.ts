// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    describe,
    expect,
    it
} from 'vitest'
import {
    PASSWORD_MIN_LENGTH,
    validateForgotPassword,
    validateResetPassword
} from './passwordResetValidation'

describe('passwordResetValidation', () => {
    describe('validateForgotPassword', () => {
        it('akzeptiert eine gültige E-Mail-Adresse', () => {
            const errors = validateForgotPassword({
                email: 'max@example.de'
            })

            expect(errors).toEqual({})
        })

        it('meldet eine fehlende E-Mail-Adresse', () => {
            const errors = validateForgotPassword({
                email: '   '
            })

            expect(errors.email).toBe(
                'Bitte gib deine E-Mail-Adresse ein.'
            )
        })

        it('meldet eine ungültige E-Mail-Adresse', () => {
            const errors = validateForgotPassword({
                email: 'keine-gueltige-email'
            })

            expect(errors.email).toBe(
                'Bitte gib eine gültige E-Mail-Adresse ein.'
            )
        })

        it('akzeptiert Leerzeichen um eine gültige E-Mail-Adresse', () => {
            const errors = validateForgotPassword({
                email: '  max@example.de  '
            })

            expect(errors).toEqual({})
        })
    })

    describe('validateResetPassword', () => {
        it('akzeptiert zwei übereinstimmende gültige Passwörter', () => {
            const errors = validateResetPassword({
                newPassword: 'MeinNeuesPasswort123',
                passwordConfirmation:
                    'MeinNeuesPasswort123'
            })

            expect(errors).toEqual({})
        })

        it('verwendet eine Mindestlänge von zwölf Zeichen', () => {
            expect(PASSWORD_MIN_LENGTH).toBe(12)
        })

        it('meldet ein fehlendes neues Passwort', () => {
            const errors = validateResetPassword({
                newPassword: '',
                passwordConfirmation: ''
            })

            expect(errors.newPassword).toBe(
                'Bitte gib ein neues Passwort ein.'
            )

            expect(errors.passwordConfirmation).toBe(
                'Bitte wiederhole das neue Passwort.'
            )
        })

        it('meldet ein zu kurzes neues Passwort', () => {
            const errors = validateResetPassword({
                newPassword: 'zu-kurz',
                passwordConfirmation: 'zu-kurz'
            })

            expect(errors.newPassword).toBe(
                'Das Passwort muss mindestens zwölf Zeichen lang sein.'
            )

            expect(
                errors.passwordConfirmation
            ).toBeUndefined()
        })

        it('akzeptiert ein Passwort mit genau zwölf Zeichen', () => {
            const password = '123456789012'

            const errors = validateResetPassword({
                newPassword: password,
                passwordConfirmation: password
            })

            expect(errors).toEqual({})
        })

        it('meldet eine fehlende Passwortwiederholung', () => {
            const errors = validateResetPassword({
                newPassword: 'MeinNeuesPasswort123',
                passwordConfirmation: ''
            })

            expect(errors.passwordConfirmation).toBe(
                'Bitte wiederhole das neue Passwort.'
            )
        })

        it('meldet nicht übereinstimmende Passwörter', () => {
            const errors = validateResetPassword({
                newPassword: 'MeinNeuesPasswort123',
                passwordConfirmation:
                    'EinAnderesPasswort123'
            })

            expect(errors.passwordConfirmation).toBe(
                'Die eingegebenen Passwörter stimmen nicht überein.'
            )
        })
    })
})