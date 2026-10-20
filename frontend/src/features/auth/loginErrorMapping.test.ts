import {describe, expect, it} from 'vitest'
import {ApiRequestError} from '../../api/restClient'
import {
    formatWaitTime,
    INVALID_CREDENTIALS_MESSAGE,
    mapLoginError,
    TECHNICAL_LOGIN_ERROR_MESSAGE
} from './loginErrorMapping'

describe('mapLoginError', () => {
    it('übersetzt INVALID_CREDENTIALS in einen allgemeinen Fehler', () => {
        const error = new ApiRequestError(401, {
            code: 'INVALID_CREDENTIALS',
            detail: 'Benutzer unbekannt'
        })

        expect(mapLoginError(error)).toEqual({
            message: INVALID_CREDENTIALS_MESSAGE
        })
    })

    it('gibt keine Details des Backends an den Nutzer weiter', () => {
        const error = new ApiRequestError(401, {
            code: 'INVALID_CREDENTIALS',
            detail: 'Kein Konto für max@example.de'
        })

        expect(mapLoginError(error).message).not.toContain('max@')
    })

    it('nennt bei 429 mit Retry-After die Wartezeit', () => {
        const error = new ApiRequestError(
            429,
            {code: 'TOO_MANY_LOGIN_ATTEMPTS'},
            60
        )

        expect(mapLoginError(error)).toEqual({
            message:
                'Es wurden zu viele Anmeldeversuche durchgeführt. Bitte versuche es in 1 Minute erneut.',
            retryAfterSeconds: 60
        })
    })

    it('verwendet bei 429 ohne Retry-After die allgemeine Meldung', () => {
        const error = new ApiRequestError(429, {})

        expect(mapLoginError(error)).toEqual({
            message:
                'Es wurden zu viele Anmeldeversuche durchgeführt. Bitte versuche es später erneut.',
            retryAfterSeconds: undefined
        })
    })

    it.each([
        new ApiRequestError(500, {code: 'INTERNAL_SERVER_ERROR'}),
        new ApiRequestError(503, {}),
        new TypeError('Failed to fetch'),
        'unbekannt'
    ])('verwendet für technische Fehler die allgemeine Meldung', (error) => {
        expect(mapLoginError(error)).toEqual({
            message: TECHNICAL_LOGIN_ERROR_MESSAGE
        })
    })
})

describe('formatWaitTime', () => {
    it.each([
        [1, '1 Sekunde'],
        [45, '45 Sekunden'],
        [60, '1 Minute'],
        [61, '2 Minuten'],
        [300, '5 Minuten']
    ])('formatiert %i Sekunden als "%s"', (seconds, expected) => {
        expect(formatWaitTime(seconds)).toBe(expected)
    })
})