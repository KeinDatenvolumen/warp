/**
 * @vitest-environment jsdom
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {
    ApiRequestError,
    deleteRequest,
    parseRetryAfter,
    postJson,
    resetCsrfTokenCache
} from './restClient'

const csrfBody = {
    headerName: 'X-XSRF-TOKEN',
    parameterName: '_csrf',
    token: 'csrf-token-1'
}

function jsonResponse(
    body: unknown,
    init: ResponseInit = {}
): Response {
    return new Response(JSON.stringify(body), {
        status: 200,
        headers: {'Content-Type': 'application/json'},
        ...init
    })
}

function noContent(): Response {
    return new Response(null, {status: 204})
}

function callAt(
    mock: ReturnType<typeof vi.fn<typeof fetch>>,
    index: number
): [input: RequestInfo | URL, init?: RequestInit] {
    const call = mock.mock.calls[index]

    if (!call) {
        throw new Error(`Kein fetch-Aufruf mit Index ${index}.`)
    }

    return call
}

describe('parseRetryAfter', () => {
    it('liest Sekundenangaben', () => {
        expect(parseRetryAfter('60')).toBe(60)
        expect(parseRetryAfter(' 5 ')).toBe(5)
    })

    it('liest HTTP-Datumsangaben relativ zum aktuellen Zeitpunkt', () => {
        const now = Date.parse('2026-10-18T10:00:00Z')

        expect(
            parseRetryAfter('Sun, 18 Oct 2026 10:00:30 GMT', now)
        ).toBe(30)
    })

    it('ignoriert fehlende, ungültige und nicht positive Werte', () => {
        const now = Date.parse('2026-10-18T10:00:00Z')

        expect(parseRetryAfter(null)).toBeUndefined()
        expect(parseRetryAfter('')).toBeUndefined()
        expect(parseRetryAfter('bald')).toBeUndefined()
        expect(parseRetryAfter('0')).toBeUndefined()
        expect(
            parseRetryAfter('Sun, 18 Oct 2026 09:00:00 GMT', now)
        ).toBeUndefined()
    })

    it('begrenzt übergroße Werte', () => {
        expect(parseRetryAfter('999999')).toBe(3600)
    })
})

describe('restClient', () => {
    const fetchMock = vi.fn<typeof fetch>()

    beforeEach(() => {
        fetchMock.mockReset()
        vi.stubGlobal('fetch', fetchMock)
        resetCsrfTokenCache()
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    describe('postJson', () => {
        it('sendet JSON mit Cookies, aber ohne CSRF-Abruf', async () => {
            fetchMock.mockResolvedValueOnce(
                jsonResponse({userId: 'usr_1'})
            )

            const result = await postJson('/api/v1/sessions', {
                email: 'max@example.de'
            })

            expect(result).toEqual({userId: 'usr_1'})
            expect(fetchMock).toHaveBeenCalledTimes(1)

            const [url, init] = callAt(fetchMock, 0)
            expect(url).toBe('/api/v1/sessions')
            expect(init?.method).toBe('POST')
            expect(init?.credentials).toBe('include')
            expect(init?.body).toBe(
                JSON.stringify({email: 'max@example.de'})
            )
            expect(init?.headers).not.toHaveProperty('X-XSRF-TOKEN')
        })

        it('übersetzt Fehlerantworten in ApiRequestError mit Retry-After', async () => {
            fetchMock.mockResolvedValueOnce(
                jsonResponse(
                    {code: 'TOO_MANY_LOGIN_ATTEMPTS'},
                    {status: 429, headers: {'Retry-After': '60'}}
                )
            )

            const error = await postJson('/api/v1/sessions', {})
                .catch((caught: unknown) => caught)

            expect(error).toBeInstanceOf(ApiRequestError)
            expect(error).toMatchObject({
                status: 429,
                code: 'TOO_MANY_LOGIN_ATTEMPTS',
                retryAfterSeconds: 60
            })
        })

        it('lässt retryAfterSeconds ohne Header leer', async () => {
            fetchMock.mockResolvedValueOnce(
                jsonResponse(
                    {code: 'INVALID_CREDENTIALS'},
                    {status: 401}
                )
            )

            const error = await postJson('/api/v1/sessions', {})
                .catch((caught: unknown) => caught)

            expect(error).toMatchObject({
                status: 401,
                code: 'INVALID_CREDENTIALS',
                retryAfterSeconds: undefined
            })
        })

        it('reicht Netzwerkfehler unverändert weiter', async () => {
            fetchMock.mockRejectedValueOnce(new TypeError('offline'))

            await expect(
                postJson('/api/v1/sessions', {})
            ).rejects.toBeInstanceOf(TypeError)
        })
    })

    describe('deleteRequest', () => {
        it('lädt den CSRF-Token und sendet ihn im Header', async () => {
            fetchMock
                .mockResolvedValueOnce(jsonResponse(csrfBody))
                .mockResolvedValueOnce(noContent())

            await expect(
                deleteRequest('/api/v1/sessions/current')
            ).resolves.toBeUndefined()

            expect(fetchMock).toHaveBeenCalledTimes(2)
            expect(callAt(fetchMock, 0)[0]).toBe('/api/v1/csrf')

            const [url, init] = callAt(fetchMock, 1)
            expect(url).toBe('/api/v1/sessions/current')
            expect(init?.method).toBe('DELETE')
            expect(init?.credentials).toBe('include')
            expect(init?.headers).toMatchObject({
                'X-XSRF-TOKEN': 'csrf-token-1'
            })
        })

        it('verwendet den geladenen Token wieder', async () => {
            fetchMock
                .mockResolvedValueOnce(jsonResponse(csrfBody))
                .mockResolvedValueOnce(noContent())
                .mockResolvedValueOnce(noContent())

            await deleteRequest('/api/v1/sessions/current')
            await deleteRequest('/api/v1/sessions/current')

            const csrfCalls = fetchMock.mock.calls.filter(
                ([url]) => url === '/api/v1/csrf'
            )
            expect(csrfCalls).toHaveLength(1)
        })

        it('wiederholt bei 403 genau einmal mit einem neuen Token', async () => {
            fetchMock
                .mockResolvedValueOnce(jsonResponse(csrfBody))
                .mockResolvedValueOnce(
                    jsonResponse({}, {status: 403})
                )
                .mockResolvedValueOnce(
                    jsonResponse({...csrfBody, token: 'csrf-token-2'})
                )
                .mockResolvedValueOnce(noContent())

            await expect(
                deleteRequest('/api/v1/sessions/current')
            ).resolves.toBeUndefined()

            expect(fetchMock).toHaveBeenCalledTimes(4)
            expect(callAt(fetchMock, 3)[1]?.headers).toMatchObject({
                'X-XSRF-TOKEN': 'csrf-token-2'
            })
        })

        it('meldet einen dauerhaften 403 als ApiRequestError', async () => {
            fetchMock
                .mockResolvedValueOnce(jsonResponse(csrfBody))
                .mockResolvedValueOnce(jsonResponse({}, {status: 403}))
                .mockResolvedValueOnce(jsonResponse(csrfBody))
                .mockResolvedValueOnce(jsonResponse({}, {status: 403}))

            await expect(
                deleteRequest('/api/v1/sessions/current')
            ).rejects.toMatchObject({status: 403})
        })

        it('meldet einen fehlgeschlagenen CSRF-Abruf als Fehler', async () => {
            fetchMock.mockResolvedValueOnce(
                jsonResponse({}, {status: 503})
            )

            await expect(
                deleteRequest('/api/v1/sessions/current')
            ).rejects.toMatchObject({status: 503})
            expect(fetchMock).toHaveBeenCalledTimes(1)
        })
    })

    it('liest das Session-Cookie niemals über document.cookie', async () => {
        const cookieGetter = vi.spyOn(
            Document.prototype,
            'cookie',
            'get'
        )

        fetchMock
            .mockResolvedValueOnce(jsonResponse({userId: 'usr_1'}))
            .mockResolvedValueOnce(jsonResponse(csrfBody))
            .mockResolvedValueOnce(noContent())

        await postJson('/api/v1/sessions', {})
        await deleteRequest('/api/v1/sessions/current')

        expect(cookieGetter).not.toHaveBeenCalled()
        cookieGetter.mockRestore()
    })
})