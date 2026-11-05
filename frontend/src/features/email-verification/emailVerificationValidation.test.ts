// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
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
