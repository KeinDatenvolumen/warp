// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-199] (76b1abf) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: highlight favorites and add favorites filter to saved routes (WARP-022)
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter} from 'react-router'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import SavedRoutesPage from './SavedRoutesPage'

const useQueryMock = vi.hoisted(() => vi.fn())
const useMutationMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useQuery: useQueryMock,
    useMutation: useMutationMock
}))

const route = {
    id: 'route_1',
    name: 'Fahrradtour',
    start: {label: 'Mosbach'},
    destination: {label: 'Bad Mergentheim'},
    activityProfile: {code: 'BICYCLE', displayName: 'Fahrrad'},
    weatherScore: 78,
    createdAt: '2026-10-21T10:00:00Z'
}

function queryResult(items: unknown[], totalPages = 1) {
    return {
        data: {
            savedRoutes: {
                items,
                pageInfo: {
                    page: 0,
                    size: 20,
                    totalElements: items.length,
                    totalPages
                }
            }
        },
        loading: false,
        error: undefined,
        refetch: vi.fn()
    }
}

function renderPage() {
    return render(
        <MemoryRouter>
            <SavedRoutesPage/>
        </MemoryRouter>
    )
}

describe('SavedRoutesPage', () => {
    const deleteMock = vi.fn()

    beforeEach(() => {
        deleteMock.mockReset()
        deleteMock.mockResolvedValue({
            data: {
                deleteSavedRoute: {deletedRouteId: 'route_1', errors: []}
            }
        })
        useMutationMock.mockReturnValue([deleteMock, {loading: false}])
    })

    it('zeigt Name, Start, Ziel und verlinkt die Route', () => {
        useQueryMock.mockReturnValue(queryResult([route]))

        renderPage()

        const link = screen.getByRole('link', {name: /Fahrradtour/})

        expect(link).toHaveAttribute('href', '/saved-routes/route_1')
        expect(link).toHaveTextContent('Mosbach')
        expect(link).toHaveTextContent('Bad Mergentheim')
    })

    it('zeigt einen Leerzustand', () => {
        useQueryMock.mockReturnValue(queryResult([], 0))

        renderPage()

        expect(
            screen.getByText(/noch keine Route gespeichert/)
        ).toBeInTheDocument()
    })

    it('zeigt einen Fehler beim Laden', () => {
        useQueryMock.mockReturnValue({
            data: undefined,
            loading: false,
            error: new Error('boom'),
            refetch: vi.fn()
        })

        renderPage()

        expect(screen.getByRole('alert')).toHaveTextContent(
            'konnten nicht geladen werden'
        )
    })

    it('löscht erst nach der Sicherheitsabfrage', async () => {
        useQueryMock.mockReturnValue(queryResult([route]))
        const user = userEvent.setup()

        renderPage()

        await user.click(
            screen.getByRole('button', {name: 'Route Fahrradtour löschen'})
        )

        expect(deleteMock).not.toHaveBeenCalled()

        await user.click(
            screen.getByRole('button', {name: 'Endgültig löschen'})
        )

        expect(deleteMock).toHaveBeenCalledWith(
            expect.objectContaining({variables: {id: 'route_1'}})
        )
    })

    it('löscht nichts, wenn die Abfrage abgebrochen wird', async () => {
        useQueryMock.mockReturnValue(queryResult([route]))
        const user = userEvent.setup()

        renderPage()

        await user.click(
            screen.getByRole('button', {name: 'Route Fahrradtour löschen'})
        )
        await user.click(screen.getByRole('button', {name: 'Abbrechen'}))

        expect(deleteMock).not.toHaveBeenCalled()
    })

    it('hebt Favoriten hervor', () => {
        useQueryMock.mockReturnValue(
            queryResult([
                {...route, favorite: true},
                {...route, id: 'route_2', name: 'Wanderung', favorite: false}
            ])
        )

        renderPage()

        expect(
            screen.getByRole('button', {
                name: 'Route Fahrradtour als Favorit markieren'
            })
        ).toHaveAttribute('aria-pressed', 'true')
        expect(
            screen.getByRole('button', {
                name: 'Route Wanderung als Favorit markieren'
            })
        ).toHaveAttribute('aria-pressed', 'false')
    })

    it('filtert auf Favoriten und springt auf Seite 1', async () => {
        useQueryMock.mockReturnValue(queryResult([route]))
        const user = userEvent.setup()

        renderPage()

        await user.click(
            screen.getByRole('button', {name: 'Nur Favoriten'})
        )

        expect(useQueryMock).toHaveBeenLastCalledWith(
            expect.anything(),
            expect.objectContaining({
                variables: {
                    filter: {favorite: true},
                    page: {page: 0, size: 20}
                }
            })
        )
    })

    it('zeigt einen eigenen Leerzustand für Favoriten', async () => {
        useQueryMock.mockReturnValue(queryResult([], 0))
        const user = userEvent.setup()

        renderPage()

        await user.click(
            screen.getByRole('button', {name: 'Nur Favoriten'})
        )

        expect(
            screen.getByText(/noch keine Route als Favorit markiert/)
        ).toBeInTheDocument()
    })
})