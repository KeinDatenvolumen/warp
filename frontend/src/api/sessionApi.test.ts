// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
/**
 * @vitest-environment jsdom
 */

import {beforeEach, describe, expect, it, vi} from 'vitest'
import {apolloClient} from './apolloClient'
import {deleteRequest, postJson} from './restClient'
import {
    createSession,
    deleteCurrentSession,
    fetchCurrentSession
} from './sessionApi'

vi.mock('./apolloClient', () => ({
    apolloClient: {query: vi.fn()}
}))

vi.mock('./restClient', () => ({
    postJson: vi.fn(),
    deleteRequest: vi.fn()
}))

const mockedQuery = vi.mocked(apolloClient.query)
const mockedPostJson = vi.mocked(postJson)
const mockedDeleteRequest = vi.mocked(deleteRequest)

const session = {
    userId: 'usr_123',
    createdAt: '2026-10-18T08:00:00Z',
    expiresAt: '2026-10-25T08:00:00Z'
}

function graphQlError(code: string) {
    return {
        message: 'Fehler',
        errors: [{message: 'Fehler', extensions: {code}}]
    }
}

describe('sessionApi', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('meldet über POST /api/v1/sessions an', async () => {
        mockedPostJson.mockResolvedValueOnce({
            userId: 'usr_123',
            expiresAt: session.expiresAt
        })

        await createSession({
            email: 'max@example.de',
            password: 'MeinSicheresPasswort123'
        })

        expect(mockedPostJson).toHaveBeenCalledWith(
            '/api/v1/sessions',
            {
                email: 'max@example.de',
                password: 'MeinSicheresPasswort123'
            }
        )
    })

    it('meldet über DELETE /api/v1/sessions/current ab', async () => {
        mockedDeleteRequest.mockResolvedValueOnce(undefined)

        await deleteCurrentSession()

        expect(mockedDeleteRequest).toHaveBeenCalledWith(
            '/api/v1/sessions/current'
        )
    })

    describe('fetchCurrentSession', () => {
        it('liefert die Sitzung einer gültigen Anmeldung', async () => {
            mockedQuery.mockResolvedValueOnce({
                data: {currentSession: session}
            } as never)

            await expect(fetchCurrentSession()).resolves.toEqual(
                session
            )

            expect(mockedQuery).toHaveBeenCalledWith(
                expect.objectContaining({
                    fetchPolicy: 'no-cache',
                    errorPolicy: 'all'
                })
            )
        })

        it.each(['SESSION_INVALID', 'NOT_AUTHENTICATED'])(
            'liefert bei %s null',
            async (code) => {
                mockedQuery.mockResolvedValueOnce({
                    data: undefined,
                    error: graphQlError(code)
                } as never)

                await expect(fetchCurrentSession()).resolves.toBeNull()
            }
        )

        it('gibt technische GraphQL-Fehler weiter', async () => {
            const error = graphQlError('INTERNAL_SERVER_ERROR')

            mockedQuery.mockResolvedValueOnce({
                data: undefined,
                error
            } as never)

            await expect(fetchCurrentSession()).rejects.toBe(error)
        })

        it('gibt Netzwerkfehler weiter', async () => {
            const error = new TypeError('offline')
            mockedQuery.mockRejectedValueOnce(error)

            await expect(fetchCurrentSession()).rejects.toBe(error)
        })
    })
})