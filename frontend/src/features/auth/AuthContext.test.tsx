// [KI-Log Nr. KI-126] (8495425) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: manage session status centrally and implement login form (WARP-006)
// [KI-Log Nr. KI-234] (d6e6dbb) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): fix(frontend): prevent sessionInvalidLink and clearStore from cancelling startup queries when unauthenticated (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {act, render, screen, waitFor} from '@testing-library/react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {clearApolloCache} from '../../api/apolloClient'
import {
    createSession,
    deleteCurrentSession,
    fetchCurrentSession
} from '../../api/sessionApi'
import type {CurrentSession} from '../../types/session'
import {AuthProvider, type AuthContextValue} from './AuthContext'
import {SessionNotEstablishedError} from './authErrors'
import {notifySessionInvalid} from './sessionEvents'
import {useAuth} from './useAuth'

vi.mock('../../api/sessionApi', () => ({
    createSession: vi.fn(),
    deleteCurrentSession: vi.fn(),
    fetchCurrentSession: vi.fn()
}))

vi.mock('../../api/apolloClient', () => ({
    clearApolloCache: vi.fn()
}))

const mockedCreateSession = vi.mocked(createSession)
const mockedDeleteCurrentSession = vi.mocked(deleteCurrentSession)
const mockedFetchCurrentSession = vi.mocked(fetchCurrentSession)
const mockedClearApolloCache = vi.mocked(clearApolloCache)

const session: CurrentSession = {
    userId: 'usr_123',
    createdAt: '2026-10-18T08:00:00Z',
    expiresAt: '2026-10-25T08:00:00Z'
}

const credentials = {
    email: 'max@example.de',
    password: 'MeinSicheresPasswort123'
}

let auth: AuthContextValue

function Probe() {
    auth = useAuth()

    return (
        <p data-testid="status">
            {auth.status}:{auth.session?.userId ?? '-'}
        </p>
    )
}

function renderProvider() {
    return render(
        <AuthProvider>
            <Probe/>
        </AuthProvider>
    )
}

describe('AuthProvider', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockedClearApolloCache.mockResolvedValue(undefined)
        mockedCreateSession.mockResolvedValue({
            userId: session.userId,
            expiresAt: session.expiresAt
        })
        mockedDeleteCurrentSession.mockResolvedValue(undefined)
    })

    describe('Sitzungsprüfung beim Start', () => {
        it('beginnt im Zustand "loading"', () => {
            mockedFetchCurrentSession.mockReturnValue(
                new Promise(() => undefined)
            )

            renderProvider()

            expect(screen.getByTestId('status')).toHaveTextContent(
                'loading:-'
            )
            expect(auth.isLoading).toBe(true)
            expect(auth.isAuthenticated).toBe(false)
        })

        it('wechselt bei gültiger Sitzung zu "authenticated"', async () => {
            mockedFetchCurrentSession.mockResolvedValueOnce(session)

            renderProvider()

            await waitFor(() =>
                expect(screen.getByTestId('status')).toHaveTextContent(
                    'authenticated:usr_123'
                )
            )
            expect(auth.isAuthenticated).toBe(true)
        })

        it('wechselt bei ungültiger Sitzung zu "unauthenticated"', async () => {
            mockedFetchCurrentSession.mockResolvedValueOnce(null)

            renderProvider()

            await waitFor(() =>
                expect(screen.getByTestId('status')).toHaveTextContent(
                    'unauthenticated:-'
                )
            )
        })

        it('sperrt bei technischen Fehlern geschützte Inhalte', async () => {
            mockedFetchCurrentSession.mockRejectedValueOnce(
                new TypeError('offline')
            )

            renderProvider()

            await waitFor(() =>
                expect(screen.getByTestId('status')).toHaveTextContent(
                    'unauthenticated:-'
                )
            )
        })
    })

    describe('login', () => {
        beforeEach(() => {
            mockedFetchCurrentSession.mockResolvedValueOnce(null)
        })

        it('aktualisiert den Status nach erfolgreicher Anmeldung', async () => {
            renderProvider()
            await waitFor(() =>
                expect(auth.status).toBe('unauthenticated')
            )

            mockedFetchCurrentSession.mockResolvedValueOnce(session)

            await act(async () => {
                await auth.login(credentials)
            })

            expect(mockedCreateSession).toHaveBeenCalledWith(credentials)
            expect(screen.getByTestId('status')).toHaveTextContent(
                'authenticated:usr_123'
            )
        })

        it('bleibt bei einem Anmeldefehler abgemeldet', async () => {
            renderProvider()
            await waitFor(() =>
                expect(auth.status).toBe('unauthenticated')
            )

            const failure = new Error('abgelehnt')
            mockedCreateSession.mockRejectedValueOnce(failure)

            await act(async () => {
                await expect(auth.login(credentials)).rejects.toBe(
                    failure
                )
            })

            expect(auth.status).toBe('unauthenticated')
        })

        it('meldet einen Fehler, wenn die Sitzung nicht bestätigt wird', async () => {
            renderProvider()
            await waitFor(() =>
                expect(auth.status).toBe('unauthenticated')
            )

            mockedFetchCurrentSession.mockResolvedValueOnce(null)

            await act(async () => {
                await expect(auth.login(credentials)).rejects.toBeInstanceOf(
                    SessionNotEstablishedError
                )
            })

            expect(auth.status).toBe('unauthenticated')
        })
    })

    describe('logout', () => {
        beforeEach(() => {
            mockedFetchCurrentSession.mockResolvedValueOnce(session)
        })

        it('setzt den Status zurück und leert benutzerbezogene Daten', async () => {
            renderProvider()
            await waitFor(() =>
                expect(auth.status).toBe('authenticated')
            )

            await act(async () => {
                await auth.logout()
            })

            expect(mockedDeleteCurrentSession).toHaveBeenCalledTimes(1)
            expect(mockedClearApolloCache).toHaveBeenCalledTimes(1)
            expect(screen.getByTestId('status')).toHaveTextContent(
                'unauthenticated:-'
            )
        })

        it('bleibt angemeldet, wenn die Abmeldung fehlschlägt', async () => {
            renderProvider()
            await waitFor(() =>
                expect(auth.status).toBe('authenticated')
            )

            const failure = new Error('nicht erreichbar')
            mockedDeleteCurrentSession.mockRejectedValueOnce(failure)

            await act(async () => {
                await expect(auth.logout()).rejects.toBe(failure)
            })

            expect(auth.status).toBe('authenticated')
            expect(mockedClearApolloCache).not.toHaveBeenCalled()
        })
    })

    describe('Sitzungsablauf', () => {
        it('wechselt bei SESSION_INVALID automatisch in den abgemeldeten Zustand', async () => {
            mockedFetchCurrentSession.mockResolvedValueOnce(session)

            renderProvider()
            await waitFor(() =>
                expect(auth.status).toBe('authenticated')
            )

            act(() => {
                notifySessionInvalid()
            })

            expect(screen.getByTestId('status')).toHaveTextContent(
                'unauthenticated:-'
            )
            expect(mockedClearApolloCache).toHaveBeenCalledTimes(1)
        })

        it('ignoriert SESSION_INVALID wenn der Benutzer bereits unauthenticated ist', async () => {
            mockedFetchCurrentSession.mockResolvedValueOnce(null)

            renderProvider()
            await waitFor(() =>
                expect(auth.status).toBe('unauthenticated')
            )

            act(() => {
                notifySessionInvalid()
            })

            expect(mockedClearApolloCache).not.toHaveBeenCalled()
            expect(screen.getByTestId('status')).toHaveTextContent(
                'unauthenticated:-'
            )
        })

        it('überschreibt eine Anmeldung nicht mit dem späten Startergebnis', async () => {
            let resolveStartup!: (value: CurrentSession | null) => void
            mockedFetchCurrentSession.mockReturnValueOnce(
                new Promise((resolve) => {
                    resolveStartup = resolve
                })
            )

            renderProvider()

            mockedFetchCurrentSession.mockResolvedValueOnce(session)

            await act(async () => {
                await auth.login(credentials)
            })
            expect(auth.status).toBe('authenticated')

            await act(async () => {
                resolveStartup(null)
            })

            expect(auth.status).toBe('authenticated')
        })
    })

    it('entfernt den Listener beim Entfernen des Providers', async () => {
        mockedFetchCurrentSession.mockResolvedValueOnce(session)

        const {unmount} = renderProvider()
        await waitFor(() => expect(auth.status).toBe('authenticated'))

        unmount()
        notifySessionInvalid()

        expect(mockedClearApolloCache).not.toHaveBeenCalled()
    })
})