import {describe, expect, it} from 'vitest'
import {mapConfirmError, mapResendError} from './emailVerificationErrorMapping'

describe('emailVerificationErrorMapping', () => {
    describe('mapConfirmError', () => {
        it('behandelt INVALID_VERIFICATION_TOKEN und 400', () => {
            expect(mapConfirmError(400, 'INVALID_VERIFICATION_TOKEN')).toBe(
                'Der Bestätigungslink ist ungültig, abgelaufen oder wurde bereits verwendet.'
            )
            expect(mapConfirmError(400)).toBe(
                'Der Bestätigungslink ist ungültig, abgelaufen oder wurde bereits verwendet.'
            )
        })

        it('behandelt 429 Rate Limiting', () => {
            expect(mapConfirmError(429)).toBe(
                'Zu viele Anfragen. Bitte versuche es später erneut.'
            )
        })

        it('nutzt Standardmeldung oder Fallback bei anderen Fehlern', () => {
            expect(mapConfirmError(500, undefined, 'Serverfehler')).toBe(
                'Serverfehler'
            )
            expect(mapConfirmError(500)).toBe(
                'Die E-Mail-Adresse konnte nicht bestätigt werden. Bitte versuche es erneut.'
            )
        })
    })

    describe('mapResendError', () => {
        it('behandelt 429 Rate Limiting', () => {
            expect(mapResendError(429)).toBe(
                'Zu viele Anfragen. Bitte versuche es später erneut.'
            )
        })

        it('nutzt Standardmeldung oder Fallback bei anderen Fehlern', () => {
            expect(mapResendError(500, undefined, 'Fehler')).toBe('Fehler')
            expect(mapResendError(500)).toBe(
                'Die Bestätigungs-E-Mail konnte nicht erneut gesendet werden. Bitte versuche es später erneut.'
            )
        })
    })
})
