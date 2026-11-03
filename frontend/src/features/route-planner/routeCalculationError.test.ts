import { describe, expect, it } from 'vitest'
import {
    parseCalculationStatusError,
    parseRouteCalculationError,
} from './routeCalculationError'

describe('routeCalculationError', () => {
    describe('parseRouteCalculationError', () => {
        it('returns action for SESSION_INVALID GraphQL error (Guest user)', () => {
            const guestGraphqlError = {
                graphQLErrors: [
                    {
                        message: 'Die Sitzung ist ungültig oder abgelaufen.',
                        extensions: {
                            code: 'SESSION_INVALID',
                            classification: 'UNAUTHORIZED',
                        },
                    },
                ],
            }

            const parsed = parseRouteCalculationError(guestGraphqlError)
            expect(parsed.code).toBe('SESSION_INVALID')
            expect(parsed.title).toBe('Anmeldung erforderlich')
            expect(parsed.message).toContain('Anmeldung erforderlich')
            expect(parsed.action?.to).toBe('/login')
            expect(parsed.secondaryAction?.to).toBe('/register')
        })

        it('returns action for EMAIL_NOT_VERIFIED', () => {
            const unverifiedPayload = [
                {
                    code: 'EMAIL_NOT_VERIFIED',
                    message: 'Die E-Mail-Adresse ist noch nicht verifiziert.',
                },
            ]

            const parsed = parseRouteCalculationError(unverifiedPayload)
            expect(parsed.code).toBe('EMAIL_NOT_VERIFIED')
            expect(parsed.title).toBe('E-Mail-Bestätigung erforderlich')
            expect(parsed.action?.to).toBe('/verify-email')
        })

        it('returns friendly message for INVALID_ROUTE_REQUEST', () => {
            const invalidRequest = {
                errors: [
                    {
                        extensions: {
                            code: 'INVALID_ROUTE_REQUEST',
                        },
                    },
                ],
            }

            const parsed = parseRouteCalculationError(invalidRequest)
            expect(parsed.code).toBe('INVALID_ROUTE_REQUEST')
            expect(parsed.title).toBe('Ungültige Routenanfrage')
        })

        it('returns friendly message for ROUTING_SERVICE_UNAVAILABLE', () => {
            const unavailable = {
                graphQLErrors: [
                    {
                        extensions: {
                            code: 'ROUTING_SERVICE_UNAVAILABLE',
                        },
                    },
                ],
            }

            const parsed = parseRouteCalculationError(unavailable)
            expect(parsed.code).toBe('ROUTING_SERVICE_UNAVAILABLE')
            expect(parsed.title).toBe('Dienst vorübergehend nicht verfügbar')
        })

        it('returns friendly message for ROUTE_NOT_FOUND', () => {
            const notFound = {
                graphQLErrors: [
                    {
                        extensions: {
                            code: 'ROUTE_NOT_FOUND',
                        },
                    },
                ],
            }

            const parsed = parseRouteCalculationError(notFound)
            expect(parsed.code).toBe('ROUTE_NOT_FOUND')
            expect(parsed.title).toBe('Keine Route gefunden')
        })

        it('returns friendly message for network errors', () => {
            const networkError = {
                networkError: new TypeError('Failed to fetch'),
            }

            const parsed = parseRouteCalculationError(networkError)
            expect(parsed.code).toBe('NETWORK_ERROR')
            expect(parsed.title).toBe('Verbindungsfehler')
            expect(parsed.message).toContain('Internetverbindung')
        })

        it('returns server message when specific code not mapped', () => {
            const custom = {
                graphQLErrors: [
                    {
                        message: 'Berechnungskontingent überschritten.',
                    },
                ],
            }

            const parsed = parseRouteCalculationError(custom)
            expect(parsed.message).toBe('Berechnungskontingent überschritten.')
        })

        it('returns fallback message for unknown error', () => {
            const parsed = parseRouteCalculationError({})
            expect(parsed.title).toBe('Fehler bei der Routenberechnung')
            expect(parsed.message).toContain('später noch einmal')
        })
    })

    describe('parseCalculationStatusError', () => {
        it('returns null if status is not FAILED', () => {
            expect(parseCalculationStatusError({ status: 'COMPLETED' })).toBeNull()
            expect(parseCalculationStatusError({ status: 'PROCESSING' })).toBeNull()
            expect(parseCalculationStatusError(null)).toBeNull()
        })

        it('handles ROUTE_NOT_FOUND in calculation result', () => {
            const parsed = parseCalculationStatusError({
                status: 'FAILED',
                error: {
                    code: 'ROUTE_NOT_FOUND',
                    message: 'Es wurde keine Route gefunden.',
                },
            })
            expect(parsed).not.toBeNull()
            expect(parsed?.code).toBe('ROUTE_NOT_FOUND')
            expect(parsed?.title).toBe('Keine Route gefunden')
        })

        it('handles ROUTING_SERVICE_UNAVAILABLE in calculation result', () => {
            const parsed = parseCalculationStatusError({
                status: 'FAILED',
                error: {
                    code: 'ROUTING_SERVICE_UNAVAILABLE',
                    message: 'Routendienst vorübergehend nicht verfügbar.',
                },
            })
            expect(parsed).not.toBeNull()
            expect(parsed?.code).toBe('ROUTING_SERVICE_UNAVAILABLE')
            expect(parsed?.title).toBe('Routendienst nicht verfügbar')
        })

        it('handles generic calculation failure', () => {
            const parsed = parseCalculationStatusError({
                status: 'FAILED',
                error: {
                    code: 'UNKNOWN',
                    message: 'Fehler bei der Berechnung.',
                },
            })
            expect(parsed).not.toBeNull()
            expect(parsed?.title).toBe('Routenberechnung fehlgeschlagen')
            expect(parsed?.message).toBe('Fehler bei der Berechnung.')
        })
    })
})
