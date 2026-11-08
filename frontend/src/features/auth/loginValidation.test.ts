// [KI-Log Nr. KI-125] (d8c8a50) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add login validation, error mapping, redirect target and countdown helper (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {validateLogin} from './loginValidation'

describe('validateLogin', () => {
    it('akzeptiert gültige Eingaben', () => {
        expect(
            validateLogin({
                email: 'max@example.de',
                password: 'irgendein-passwort'
            })
        ).toEqual({})
    })

    it('meldet leere Pflichtfelder', () => {
        expect(validateLogin({email: '', password: ''})).toEqual({
            email: 'Bitte gib deine E-Mail-Adresse ein.',
            password: 'Bitte gib dein Passwort ein.'
        })
    })

    it('behandelt eine E-Mail-Adresse nur aus Leerzeichen als leer', () => {
        expect(
            validateLogin({email: '   ', password: 'x'}).email
        ).toBe('Bitte gib deine E-Mail-Adresse ein.')
    })

    it.each(['max', 'max@', 'max@example', '@example.de', 'm ax@example.de'])(
        'erkennt die formal ungültige E-Mail-Adresse "%s"',
        (email) => {
            expect(
                validateLogin({email, password: 'x'}).email
            ).toBe('Bitte gib eine gültige E-Mail-Adresse ein.')
        }
    )

    it('ignoriert umgebende Leerzeichen der E-Mail-Adresse', () => {
        expect(
            validateLogin({email: ' max@example.de ', password: 'x'})
        ).toEqual({})
    })

    it('prüft beim Passwort keine Mindestlänge', () => {
        expect(
            validateLogin({email: 'max@example.de', password: 'a'})
                .password
        ).toBeUndefined()
    })

    it('akzeptiert ein Passwort aus Leerzeichen', () => {
        expect(
            validateLogin({email: 'max@example.de', password: '   '})
                .password
        ).toBeUndefined()
    })
})