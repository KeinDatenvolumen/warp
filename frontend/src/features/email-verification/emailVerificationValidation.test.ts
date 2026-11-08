// [KI-Log Nr. KI-167] (af1ca24) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: add frontend email verification (WARP-007)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {validateResendEmail} from './emailVerificationValidation'

describe('emailVerificationValidation', () => {
    it('lehnt leere Eingabe ab', () => {
        expect(validateResendEmail('')).toBe(
            'Bitte gib deine E-Mail-Adresse ein.'
        )
        expect(validateResendEmail('   ')).toBe(
            'Bitte gib deine E-Mail-Adresse ein.'
        )
    })

    it('lehnt ungültige E-Mail-Formate ab', () => {
        expect(validateResendEmail('keine-email')).toBe(
            'Bitte gib eine gültige E-Mail-Adresse ein.'
        )
        expect(validateResendEmail('test@')).toBe(
            'Bitte gib eine gültige E-Mail-Adresse ein.'
        )
        expect(validateResendEmail('@domain.de')).toBe(
            'Bitte gib eine gültige E-Mail-Adresse ein.'
        )
        expect(validateResendEmail('test@domain')).toBe(
            'Bitte gib eine gültige E-Mail-Adresse ein.'
        )
    })

    it('akzeptiert gültige E-Mail-Adressen', () => {
        expect(validateResendEmail('user@example.de')).toBeUndefined()
        expect(validateResendEmail('  max.mustermann@warp.local  ')).toBeUndefined()
    })
})
