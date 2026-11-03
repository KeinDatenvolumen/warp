import { describe, expect, it } from 'vitest'
import {
    containsSessionInvalidCode,
    extractGraphQlErrorCodes,
    extractGraphQlErrorMessage,
    isApolloInternalUrlMessage,
    isNetworkError,
} from './graphqlErrors'

describe('graphqlErrors', () => {
    describe('extractGraphQlErrorCodes', () => {
        it('returns empty array for non-objects or empty structures', () => {
            expect(extractGraphQlErrorCodes(null)).toEqual([])
            expect(extractGraphQlErrorCodes(undefined)).toEqual([])
            expect(extractGraphQlErrorCodes('string')).toEqual([])
            expect(extractGraphQlErrorCodes(123)).toEqual([])
            expect(extractGraphQlErrorCodes({})).toEqual([])
        })

        it('extracts code from raw array of GraphQLErrors', () => {
            const errors = [
                {
                    message: 'Unauthorized',
                    extensions: { code: 'SESSION_INVALID', classification: 'UNAUTHORIZED' },
                },
            ]
            expect(extractGraphQlErrorCodes(errors)).toEqual(
                expect.arrayContaining(['SESSION_INVALID', 'UNAUTHORIZED'])
            )
        })

        it('extracts code from payload errors array with code property', () => {
            const payloadErrors = [
                {
                    field: null,
                    code: 'EMAIL_NOT_VERIFIED',
                    message: 'Die E-Mail ist nicht verifiziert.',
                },
            ]
            expect(extractGraphQlErrorCodes(payloadErrors)).toEqual(['EMAIL_NOT_VERIFIED'])
        })

        it('extracts code from standard GraphQL response object', () => {
            const response = {
                errors: [
                    {
                        message: 'Downstream down',
                        extensions: { code: 'ROUTING_SERVICE_UNAVAILABLE' },
                    },
                ],
            }
            expect(extractGraphQlErrorCodes(response)).toEqual(['ROUTING_SERVICE_UNAVAILABLE'])
        })

        it('extracts code from ApolloError with graphQLErrors', () => {
            const apolloError = {
                name: 'ApolloError',
                message: 'Die Sitzung ist ungültig oder abgelaufen.',
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
            expect(extractGraphQlErrorCodes(apolloError)).toEqual(
                expect.arrayContaining(['SESSION_INVALID', 'UNAUTHORIZED'])
            )
        })

        it('extracts UNAUTHORIZED from networkError with statusCode 401', () => {
            const apolloError = {
                networkError: {
                    statusCode: 401,
                },
            }
            expect(extractGraphQlErrorCodes(apolloError)).toEqual(['UNAUTHORIZED'])
        })

        it('extracts code from networkError.result.errors', () => {
            const apolloError = {
                networkError: {
                    result: {
                        errors: [
                            {
                                extensions: { code: 'SESSION_INVALID' },
                            },
                        ],
                    },
                },
            }
            expect(extractGraphQlErrorCodes(apolloError)).toEqual(['SESSION_INVALID'])
        })
    })

    describe('containsSessionInvalidCode', () => {
        it('returns true when SESSION_INVALID or UNAUTHORIZED or NOT_AUTHENTICATED is present', () => {
            expect(
                containsSessionInvalidCode({
                    graphQLErrors: [{ extensions: { code: 'SESSION_INVALID' } }],
                })
            ).toBe(true)

            expect(
                containsSessionInvalidCode({
                    errors: [{ extensions: { classification: 'UNAUTHORIZED' } }],
                })
            ).toBe(true)

            expect(
                containsSessionInvalidCode([{ code: 'NOT_AUTHENTICATED' }])
            ).toBe(true)
        })

        it('returns false for other codes', () => {
            expect(
                containsSessionInvalidCode({
                    errors: [{ extensions: { code: 'EMAIL_NOT_VERIFIED' } }],
                })
            ).toBe(false)
        })
    })

    describe('isApolloInternalUrlMessage', () => {
        it('identifies Apollo minified error URLs', () => {
            expect(
                isApolloInternalUrlMessage(
                    'An error occurred! For more details, see the full error text at https://go.apollo.dev/c/err#%7B%22version%22%3A%224.3.5%22%2C%22message%22%3A96%2C%22args%22%3A%5B%5D%7D'
                )
            ).toBe(true)
            expect(isApolloInternalUrlMessage('Invariant Violation: 96')).toBe(true)
            expect(isApolloInternalUrlMessage('Der Dienst ist offline.')).toBe(false)
        })
    })

    describe('isNetworkError', () => {
        it('detects network errors', () => {
            expect(isNetworkError({ networkError: new Error('Failed to fetch') })).toBe(true)
            expect(isNetworkError(new TypeError('Failed to fetch'))).toBe(true)
            expect(isNetworkError({ message: 'Network error: Connection refused' })).toBe(true)
            expect(isNetworkError({ message: 'Some regular business error' })).toBe(false)
        })
    })

    describe('extractGraphQlErrorMessage', () => {
        it('extracts clean message from graphQLErrors', () => {
            const err = {
                graphQLErrors: [{ message: 'Die Sitzung ist ungültig oder abgelaufen.' }],
            }
            expect(extractGraphQlErrorMessage(err)).toBe('Die Sitzung ist ungültig oder abgelaufen.')
        })

        it('ignores Apollo internal URLs and falls back to clean message', () => {
            const err = {
                message:
                    'An error occurred! For more details, see the full error text at https://go.apollo.dev/c/err#...',
            }
            expect(extractGraphQlErrorMessage(err)).toBeNull()
        })
    })
})
