/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter} from 'react-router'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import RoutePlannerPage from './RoutePlannerPage'

const useQueryMock = vi.hoisted(() => vi.fn())
const useLazyQueryMock = vi.hoisted(() => vi.fn())
const useMutationMock = vi.hoisted(() => vi.fn())
const useAuthMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useQuery: useQueryMock,
    useLazyQuery: useLazyQueryMock,
    useMutation: useMutationMock
}))

vi.mock('../features/auth/useAuth', () => ({
    useAuth: useAuthMock
}))

vi.mock('../components/RouteMap/RouteMap', () => ({
    default: (props: { routeSegments?: unknown[] }) => (
        <div
            data-testid="route-map"
            data-segments={props.routeSegments?.length ?? 0}
        />
    )
}))

const profiles = [
    {code: 'BICYCLE', displayName: 'Fahrrad'},
    {code: 'MOTORCYCLE', displayName: 'Motorrad'},
    {code: 'HIKING', displayName: 'Wandern'}
]

function renderPage() {
    return render(
        <MemoryRouter>
            <RoutePlannerPage />
        </MemoryRouter>
    )
}

describe('RoutePlannerPage', () => {
    beforeEach(() => {
        useQueryMock.mockReturnValue({
            data: {activityProfiles: profiles},
            loading: false,
            error: undefined
        })
        useLazyQueryMock.mockReturnValue([
            vi.fn().mockResolvedValue({data: {searchPlaces: []}}),
            {loading: false, error: undefined}
        ])
        useMutationMock.mockReturnValue([vi.fn(), {loading: false}])
        useAuthMock.mockReturnValue({isAuthenticated: true})
        window.localStorage.clear()
    })

    it('deaktiviert die Routenberechnung wenn Start oder Ziel fehlen', () => {
        renderPage()

        const calculateButton = screen.getByRole('button', {
            name: 'Route berechnen'
        })
        expect(calculateButton).toBeDisabled()
        expect(
            screen.queryByRole('heading', {name: 'Route speichern'})
        ).not.toBeInTheDocument()
    })

    it('aktiviert die Berechnung sobald Profil, Start und Ziel vorliegen und zeigt dann das Speicherformular', async () => {
        const searchPlacesMock = vi.fn().mockImplementation(({variables}) => {
            if (variables.query.includes('Mosbach')) {
                return Promise.resolve({
                    data: {
                        searchPlaces: [
                            {
                                placeId: 'p1',
                                displayName: 'Mosbach',
                                latitude: 49.35,
                                longitude: 9.15
                            }
                        ]
                    }
                })
            }
            if (variables.query.includes('Mergentheim')) {
                return Promise.resolve({
                    data: {
                        searchPlaces: [
                            {
                                placeId: 'p2',
                                displayName: 'Bad Mergentheim',
                                latitude: 49.49,
                                longitude: 9.77
                            }
                        ]
                    }
                })
            }
            return Promise.resolve({data: {searchPlaces: []}})
        })

        useLazyQueryMock.mockReturnValue([
            searchPlacesMock,
            {loading: false, error: undefined}
        ])

        const user = userEvent.setup()
        renderPage()

        // 1. Profil wählen
        await user.click(screen.getByRole('button', {name: /Fahrrad/}))

        // 2. Start eingeben und auswählen
        await user.type(screen.getByLabelText('Startpunkt'), 'Mosbach')
        const startChoice = await screen.findByRole('button', {name: 'Mosbach'})
        await user.click(startChoice)

        // 3. Ziel eingeben und auswählen
        await user.type(screen.getByLabelText('Zielpunkt'), 'Mergentheim')
        const destChoice = await screen.findByRole('button', {
            name: 'Bad Mergentheim'
        })
        await user.click(destChoice)

        // Button aktivieren und klicken
        const calculateButton = screen.getByRole('button', {
            name: 'Route berechnen'
        })
        expect(calculateButton).toBeEnabled()

        await user.click(calculateButton)

        // Formular "Route speichern" erscheint
        expect(
            screen.getByRole('heading', {name: 'Route speichern'})
        ).toBeInTheDocument()
        expect(screen.getByLabelText('Name der Route')).toBeInTheDocument()
        expect(screen.getByTestId('route-map')).toHaveAttribute(
            'data-segments',
            '1'
        )

        // Wenn Ziel geändert wird, verschwindet die speicherbare Referenz
        await user.type(screen.getByLabelText('Zielpunkt'), ' neue Suche')
        expect(
            screen.queryByRole('heading', {name: 'Route speichern'})
        ).not.toBeInTheDocument()
    })

    it('fordert zur Anmeldung auf, wenn der Benutzer nicht angemeldet ist', async () => {
        useAuthMock.mockReturnValue({isAuthenticated: false})

        const searchPlacesMock = vi.fn().mockImplementation(({variables}) => {
            if (variables.query.includes('Mosbach')) {
                return Promise.resolve({
                    data: {
                        searchPlaces: [
                            {
                                placeId: 'p1',
                                displayName: 'Mosbach',
                                latitude: 49.35,
                                longitude: 9.15
                            }
                        ]
                    }
                })
            }
            if (variables.query.includes('Mergentheim')) {
                return Promise.resolve({
                    data: {
                        searchPlaces: [
                            {
                                placeId: 'p2',
                                displayName: 'Bad Mergentheim',
                                latitude: 49.49,
                                longitude: 9.77
                            }
                        ]
                    }
                })
            }
            return Promise.resolve({data: {searchPlaces: []}})
        })

        useLazyQueryMock.mockReturnValue([
            searchPlacesMock,
            {loading: false, error: undefined}
        ])

        const user = userEvent.setup()
        renderPage()

        await user.click(screen.getByRole('button', {name: /Fahrrad/}))

        await user.type(screen.getByLabelText('Startpunkt'), 'Mosbach')
        await user.click(await screen.findByRole('button', {name: 'Mosbach'}))

        await user.type(screen.getByLabelText('Zielpunkt'), 'Mergentheim')
        await user.click(await screen.findByRole('button', {name: 'Bad Mergentheim'}))

        await user.click(screen.getByRole('button', {name: 'Route berechnen'}))

        expect(screen.getByText(/melde dich an/i)).toBeInTheDocument()
        expect(screen.queryByLabelText('Name der Route')).not.toBeInTheDocument()
    })
})
