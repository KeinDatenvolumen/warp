// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
import {
    describe,
    expect,
    it
} from 'vitest'
import {
    mapPasswordResetConfirmError,
    mapPasswordResetSendError
} from './passwordResetErrorMapping'

describe('passwordResetErrorMapping', () => {
    describe('mapPasswordResetSendError', () => {
        it('ordnet Rate Limiting einer verständlichen Meldung zu', () => {
            const message = mapPasswordResetSendError(
                429,
                'TOO_MANY_REQUESTS',
                'Interne Meldung'
            )

            expect(message).toBe(
                'Zu viele Anfragen. Bitte versuche es später erneut.'
            )
        })

        it('verwendet die übergebene Standardmeldung', () => {
            const message = mapPasswordResetSendError(
                500,
                'INTERNAL_SERVER_ERROR',
                'Der Dienst ist momentan nicht verfügbar.'
            )

            expect(message).toBe(
                'Der Dienst ist momentan nicht verfügbar.'
            )
        })

        it('verwendet eine sichere Ersatzmeldung', () => {
            const message = mapPasswordResetSendError(
                500
            )

            expect(message).toBe(
                'Die Passwort-Rücksetzungs-E-Mail konnte '
                + 'nicht versendet werden. Bitte versuche '
                + 'es später erneut.'
            )
        })
    })

    describe('mapPasswordResetConfirmError', () => {
        it('ordnet INVALID_RESET_TOKEN einer Link-Meldung zu', () => {
            const message = mapPasswordResetConfirmError(
                400,
                'INVALID_RESET_TOKEN',
                'Interne Meldung'
            )

            expect(message).toBe(
                'Der Passwort-Rücksetzungslink ist ungültig, '
                + 'abgelaufen oder wurde bereits verwendet.'
            )
        })

        it('behandelt Status 400 auch ohne Fehlercode als ungültigen Link', () => {
            const message = mapPasswordResetConfirmError(
                400
            )

            expect(message).toBe(
                'Der Passwort-Rücksetzungslink ist ungültig, '
                + 'abgelaufen oder wurde bereits verwendet.'
            )
        })

        it('ordnet PASSWORD_REQUIREMENTS_NOT_MET einer Passwortmeldung zu', () => {
            const message = mapPasswordResetConfirmError(
                422,
                'PASSWORD_REQUIREMENTS_NOT_MET',
                'Interne Meldung'
            )

            expect(message).toBe(
                'Das Passwort muss mindestens zwölf Zeichen lang sein.'
            )
        })

        it('behandelt Status 422 auch ohne Fehlercode als Passwortfehler', () => {
            const message = mapPasswordResetConfirmError(
                422
            )

            expect(message).toBe(
                'Das Passwort muss mindestens zwölf Zeichen lang sein.'
            )
        })

        it('ordnet Rate Limiting einer verständlichen Meldung zu', () => {
            const message = mapPasswordResetConfirmError(
                429,
                'TOO_MANY_REQUESTS',
                'Interne Meldung'
            )

            expect(message).toBe(
                'Zu viele Anfragen. Bitte versuche es später erneut.'
            )
        })

        it('verwendet die übergebene Standardmeldung', () => {
            const message = mapPasswordResetConfirmError(
                500,
                'INTERNAL_SERVER_ERROR',
                'Der Dienst ist momentan nicht verfügbar.'
            )

            expect(message).toBe(
                'Der Dienst ist momentan nicht verfügbar.'
            )
        })

        it('verwendet eine sichere Ersatzmeldung', () => {
            const message = mapPasswordResetConfirmError(
                500
            )

            expect(message).toBe(
                'Das Passwort konnte nicht zurückgesetzt '
                + 'werden. Bitte versuche es erneut.'
            )
        })
    })
})