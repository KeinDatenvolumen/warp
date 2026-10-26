/**
 * @vitest-environment jsdom
 */

import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RoutePlannerPage from './RoutePlannerPage'
import { ROUTE_CALCULATION_QUERY } from '../api/routeCalculationApi'

const useQueryMock = vi.hoisted(() => vi.fn())
const useLazyQueryMock = vi.hoisted(() => vi.fn())
const useMutationMock = vi.hoisted(() => vi.fn())
const useAuthMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useQuery: useQueryMock,
    useLazyQuery: useLazyQueryMock,
    useMutation: useMutationMock,
}))

vi.mock('../features/auth/useAuth', () => ({
    useAuth: useAuthMock,
}))

vi.mock('../components/RouteMap/RouteMap', () => ({
    default: (props: { geometry?: { coordinates?: unknown[] } | null; routeSegments?: unknown[] }) => (
        <div
            data-testid="route-map"
            data-has-geometry={props.geometry ? 'true' : 'false'}
            data-geometry-points={props.geometry?.coordinates?.length ?? 0}
            data-segments={props.routeSegments?.length ?? 0}
        />
    ),
}))

const profiles = [
    { code: 'BICYCLE', displayName: 'Fahrrad' },
    { code: 'MOTORCYCLE', displayName: 'Motorrad' },
    { code: 'HIKING', displayName: 'Wandern' },
]

function renderPage() {
    return render(
        <MemoryRouter>
            <RoutePlannerPage />
        </MemoryRouter>,
    )
}

describe('RoutePlannerPage', () => {
    let startCalculationMock: ReturnType<typeof vi.fn>
    let stopPollingMock: ReturnType<typeof vi.fn>
    let startPollingMock: ReturnType<typeof vi.fn>

    beforeEach(() => {
        let queryExecuted = false
        stopPollingMock = vi.fn().mockImplementation(() => {
            if (!queryExecuted) {
                throw new Error("useLazyQuery: 'stopPolling' cannot be called before executing the query.")
            }
        })
        startPollingMock = vi.fn().mockImplementation(() => {
            if (!queryExecuted) {
                throw new Error("useLazyQuery: 'startPolling' cannot be called before executing the query.")
            }
        })

        useQueryMock.mockReturnValue({
            data: { activityProfiles: profiles },
            loading: false,
            error: undefined,
        })

        startCalculationMock = vi.fn().mockResolvedValue({
            data: {
                startRouteCalculation: {
                    calculation: {
                        id: 'calc_test_123',
                        status: 'COMPLETED',
                    },
                    errors: [],
                },
            },
        })

        useMutationMock.mockReturnValue([startCalculationMock, { loading: false }])

        const calculationData = {
            routeCalculation: {
                id: 'calc_test_123',
                status: 'COMPLETED',
                start: { label: 'Mosbach', latitude: 49.35, longitude: 9.15 },
                destination: { label: 'Bad Mergentheim', latitude: 49.49, longitude: 9.77 },
                activityProfile: { code: 'BICYCLE', displayName: 'Fahrrad' },
                departureTime: '2026-10-27T10:00:00Z',
                arrivalTime: '2026-10-27T12:30:00Z',
                distanceMeters: 61400,
                durationSeconds: 9000,
                geometry: {
                    type: 'LineString',
                    coordinates: [
                        [9.15, 49.35],
                        [9.77, 49.49],
                    ],
                },
                createdAt: '2026-10-26T12:00:00Z',
                error: null,
            },
        }

        const placesQueryMock = vi.fn().mockImplementation(({ variables }) => {
            if (variables?.query?.includes('Mosbach')) {
                return Promise.resolve({
                    data: {
                        searchPlaces: [
                            {
                                placeId: 'p1',
                                displayName: 'Mosbach',
                                latitude: 49.35,
                                longitude: 9.15,
                            },
                        ],
                    },
                })
            }
            if (variables?.query?.includes('Mergentheim')) {
                return Promise.resolve({
                    data: {
                        searchPlaces: [
                            {
                                placeId: 'p2',
                                displayName: 'Bad Mergentheim',
                                latitude: 49.49,
                                longitude: 9.77,
                            },
                        ],
                    },
                })
            }
            return Promise.resolve({ data: { searchPlaces: [] } })
        })

        const placesLazyResult = {
            data: undefined,
            loading: false,
            error: undefined,
            startPolling: vi.fn(),
            stopPolling: vi.fn(),
        }

        const calculationLazyResult = {
            data: calculationData,
            loading: false,
            error: undefined,
            startPolling: startPollingMock,
            stopPolling: stopPollingMock,
        }

        const loadCalculationMock = vi.fn().mockImplementation(() => {
            queryExecuted = true
            return Promise.resolve({ data: calculationData })
        })

        useLazyQueryMock.mockImplementation((query) => {
            if (query === ROUTE_CALCULATION_QUERY) {
                return [loadCalculationMock, calculationLazyResult]
            }
            return [placesQueryMock, placesLazyResult]
        })

        useAuthMock.mockReturnValue({ isAuthenticated: true })
        window.localStorage.clear()
    })

    it('deaktiviert die Routenberechnung wenn Start oder Ziel fehlen', () => {
        renderPage()

        expect(stopPollingMock).not.toHaveBeenCalled()

        const calculateButton = screen.getByRole('button', {
            name: 'Route berechnen',
        })
        expect(calculateButton).toBeDisabled()
        expect(
            screen.queryByRole('heading', { name: 'Route speichern' }),
        ).not.toBeInTheDocument()
    })

    it('validiert den Vorhersagezeitraum bei der Abfahrtszeit', () => {
        renderPage()

        const departureInput = screen.getByLabelText('Abfahrtszeit')
        expect(departureInput).toHaveAttribute('step', '900')

        // Eingabe in der Vergangenheit
        fireEvent.change(departureInput, { target: { value: '2020-01-01T10:00' } })

        expect(
            screen.getByText('Die Abfahrtszeit muss in der Zukunft liegen.'),
        ).toBeInTheDocument()

        const calculateButton = screen.getByRole('button', {
            name: 'Route berechnen',
        })
        expect(calculateButton).toBeDisabled()
    })

    it('berechnet eine Route, zeigt Ergebnisdaten, Geometrie und das Speicherformular', async () => {
        const user = userEvent.setup()
        renderPage()

        // 1. Profil wählen
        await user.click(screen.getByRole('button', { name: /Fahrrad/ }))

        // 2. Start eingeben und auswählen
        await user.type(screen.getByLabelText('Startpunkt'), 'Mosbach')
        const startChoice = await screen.findByRole('button', { name: 'Mosbach' })
        await user.click(startChoice)

        // 3. Ziel eingeben und auswählen
        await user.type(screen.getByLabelText('Zielpunkt'), 'Mergentheim')
        const destChoice = await screen.findByRole('button', {
            name: 'Bad Mergentheim',
        })
        await user.click(destChoice)

        // 4. Button aktivieren und klicken
        const calculateButton = screen.getByRole('button', {
            name: 'Route berechnen',
        })
        expect(calculateButton).toBeEnabled()

        await user.click(calculateButton)

        // 5. Start Route Calculation aufgerufen
        expect(startCalculationMock).toHaveBeenCalledWith({
            variables: {
                input: expect.objectContaining({
                    activityProfile: 'BICYCLE',
                    start: {
                        label: 'Mosbach',
                        latitude: 49.35,
                        longitude: 9.15,
                    },
                    destination: {
                        label: 'Bad Mergentheim',
                        latitude: 49.49,
                        longitude: 9.77,
                    },
                }),
            },
        })

        // 6. Routenergebnis-Sektion mit Distanz, Fahrtdauer, Ankunftszeit
        expect(
            await screen.findByRole('heading', { name: 'Routenergebnis' }),
        ).toBeInTheDocument()
        expect(screen.getByText(/61,4 km/i)).toBeInTheDocument()
        expect(screen.getByText(/2 Std\. 30 Min\./i)).toBeInTheDocument()

        // 7. Karte zeigt Geometrie
        expect(screen.getByTestId('route-map')).toHaveAttribute(
            'data-has-geometry',
            'true',
        )

        // 8. Formular "Route speichern" erscheint mit Routenname
        expect(
            screen.getByRole('heading', { name: 'Route speichern' }),
        ).toBeInTheDocument()
        expect(screen.getByLabelText('Name der Route')).toBeInTheDocument()

        // 9. Vorherige Route wird ersetzt wenn das Ziel geändert wird
        fireEvent.change(screen.getByLabelText('Zielpunkt'), { target: { value: 'neuer Ort' } })
        expect(
            screen.queryByRole('heading', { name: 'Routenergebnis' }),
        ).not.toBeInTheDocument()
        expect(
            screen.queryByRole('heading', { name: 'Route speichern' }),
        ).not.toBeInTheDocument()
    })

    it('fordert zur Anmeldung auf, wenn der Benutzer nicht angemeldet ist', async () => {
        useAuthMock.mockReturnValue({ isAuthenticated: false })

        const user = userEvent.setup()
        renderPage()

        await user.click(screen.getByRole('button', { name: /Fahrrad/ }))

        await user.type(screen.getByLabelText('Startpunkt'), 'Mosbach')
        await user.click(await screen.findByRole('button', { name: 'Mosbach' }))

        await user.type(screen.getByLabelText('Zielpunkt'), 'Mergentheim')
        await user.click(await screen.findByRole('button', { name: 'Bad Mergentheim' }))

        await user.click(screen.getByRole('button', { name: 'Route berechnen' }))

        expect(await screen.findByText(/melde dich an/i)).toBeInTheDocument()
        expect(screen.queryByLabelText('Name der Route')).not.toBeInTheDocument()
    })
})
