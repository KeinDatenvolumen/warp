// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-199] (76b1abf) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: highlight favorites and add favorites filter to saved routes (WARP-022)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import {MemoryRouter, Route, Routes} from 'react-router'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import SavedRouteDetailPage from './SavedRouteDetailPage'

const useQueryMock = vi.hoisted(() => vi.fn())
const useMutationMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useQuery: useQueryMock,
    useMutation: useMutationMock
}))

vi.mock('../components/RouteMap/RouteMap', () => ({
    default: (props: { routeSegments?: unknown[] }) => (
        <div
            data-testid="route-map"
            data-segments={props.routeSegments?.length ?? 0}
        />
    )
}))

const line = (coordinates: number[][]) => ({type: 'LineString', coordinates})

const route = {
    id: 'route_1',
    name: 'Fahrradtour',
    start: {label: 'Mosbach', latitude: 49.35, longitude: 9.15},
    destination: {label: 'Bad Mergentheim', latitude: 49.49, longitude: 9.77},
    activityProfile: {code: 'BICYCLE', displayName: 'Fahrrad'},
    departureTime: '2026-10-24T06:00:00Z',
    arrivalTime: '2026-10-24T09:12:00Z',
    distanceMeters: 57600,
    durationSeconds: 11520,
    geometry: line([[9.15, 49.35], [9.77, 49.49]]),
    weatherScore: 78,
    favorite: true,
    weatherSegments: [
        {
            segmentIndex: 0,
            score: 86,
            color: '#55AA40',
            geometry: line([[9.15, 49.35], [9.4, 49.4]])
        },
        {
            segmentIndex: 1,
            score: 70,
            color: '#AAAA40',
            geometry: line([[9.4, 49.4], [9.77, 49.49]])
        }
    ],
    createdAt: '2026-10-21T10:00:00Z'
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/saved-routes/route_1']}>
            <Routes>
                <Route
                    path="/saved-routes/:routeId"
                    element={<SavedRouteDetailPage/>}
                />
            </Routes>
        </MemoryRouter>
    )
}

describe('SavedRouteDetailPage', () => {
    beforeEach(() => {
        useMutationMock.mockReturnValue([vi.fn(), {loading: false}])
    })

    it('zeigt die Routendaten und übergibt die farbigen Abschnitte', () => {
        useQueryMock.mockReturnValue({
            data: {savedRoute: route},
            loading: false,
            error: undefined
        })

        renderPage()

        expect(
            screen.getByRole('heading', {name: 'Fahrradtour'})
        ).toBeInTheDocument()
        expect(screen.getByText('57,6 km')).toBeInTheDocument()
        expect(screen.getByText('3 Std. 12 Min.')).toBeInTheDocument()
        expect(screen.getByTestId('route-map')).toHaveAttribute(
            'data-segments',
            '2'
        )
    })

    it('zeigt eine Meldung, wenn die Route nicht existiert', () => {
        useQueryMock.mockReturnValue({
            data: {savedRoute: null},
            loading: false,
            error: undefined
        })

        renderPage()

        expect(
            screen.getByText('Diese Route wurde nicht gefunden.')
        ).toBeInTheDocument()
    })

    it('zeigt den Favoritenstatus', () => {
        useQueryMock.mockReturnValue({
            data: {savedRoute: route},
            loading: false,
            error: undefined
        })

        renderPage()

        expect(
            screen.getByRole('button', {
                name: 'Route Fahrradtour als Favorit markieren'
            })
        ).toHaveAttribute('aria-pressed', 'true')
    })
})