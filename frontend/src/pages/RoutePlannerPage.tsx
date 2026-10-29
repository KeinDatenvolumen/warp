import {
    useEffect,
    useMemo,
    useRef,
    useState,
    type ChangeEvent,
} from 'react'
import {
    useLazyQuery,
    useMutation,
} from '@apollo/client/react'
import {Link} from 'react-router'

import {SEARCH_PLACES_QUERY} from '../api/searchPlacesQuery'
import {
    ROUTE_CALCULATION_QUERY,
    START_ROUTE_CALCULATION_MUTATION,
} from '../api/routeCalculationApi'
import RouteMap from '../components/RouteMap/RouteMap'
import ActivityProfileSelector from '../features/activity-profiles/ActivityProfileSelector'
import {useAuth} from '../features/auth/useAuth'
import {
    createInitialDepartureTime,
    createMaximumDepartureTime,
    createMinimumDepartureTime,
    isDepartureTimeInFuture,
    isDepartureTimeWithinForecastPeriod,
    toDepartureTimeIso,
} from '../features/route-planner/departureTime'
import {
    formatArrivalTime,
    formatDistance,
    formatDuration,
} from '../features/route-planner/routeResultFormatting'
import SaveRouteForm from '../features/saved-routes/SaveRouteForm'
import {
    toLatLngPositions,
} from '../features/saved-routes/routeGeometry'
import type {
    ActivityProfileCode,
} from '../types/activityProfile'
import type {Place} from '../types/searchPlaces'
import type {
    RouteCalculation,
} from '../types/routeCalculation'
import type {RouteMapSegment} from '../types/routeMap'
import {isLatestRequest} from '../utils/latestRequest'
import {
    canExportRouteAsGpx,
    downloadRouteAsGpx,
} from '../features/route-planner/gpxExport'

import styles from './RoutePlannerPage.module.css'

const SELECTED_PROFILE_STORAGE_KEY =
    'warp-selected-activity-profile'

const MINIMUM_SEARCH_LENGTH = 3
const MAXIMUM_SEARCH_LENGTH = 200
const SEARCH_RESULT_LIMIT = 5
const SEARCH_DEBOUNCE_MS = 400
const ROUTE_CALCULATION_POLL_INTERVAL_MS = 1000

function readSelectedProfile(): ActivityProfileCode | null {
    if (typeof window === 'undefined') {
        return null
    }

    const value = window.localStorage.getItem(
        SELECTED_PROFILE_STORAGE_KEY,
    )

    if (
        value === 'BICYCLE' ||
        value === 'MOTORCYCLE' ||
        value === 'HIKING'
    ) {
        return value
    }

    return null
}

export default function RoutePlannerPage() {
    const {isAuthenticated} = useAuth()

    const [selectedProfile, setSelectedProfile] =
        useState<ActivityProfileCode | null>(
            readSelectedProfile,
        )

    const [startInput, setStartInput] = useState('')
    const [destinationInput, setDestinationInput] =
        useState('')

    const [departureTime, setDepartureTime] = useState(
        createInitialDepartureTime,
    )

    const [
        minimumDepartureTime,
        setMinimumDepartureTime,
    ] = useState(createMinimumDepartureTime)

    const [
        maximumDepartureTime,
        setMaximumDepartureTime,
    ] = useState(createMaximumDepartureTime)

    const [
        currentCalculationId,
        setCurrentCalculationId,
    ] = useState<string | null>(null)

    const [
        routeCalculation,
        setRouteCalculation,
    ] = useState<RouteCalculation | null>(null)

    const [
        routeCalculationError,
        setRouteCalculationError,
    ] = useState<string | null>(null)

    const [selectedStart, setSelectedStart] =
        useState<Place | null>(null)

    const [
        selectedDestination,
        setSelectedDestination,
    ] = useState<Place | null>(null)

    const [startResults, setStartResults] =
        useState<Place[]>([])

    const [
        destinationResults,
        setDestinationResults,
    ] = useState<Place[]>([])

    const [
        lastCompletedStartQuery,
        setLastCompletedStartQuery,
    ] = useState<string | null>(null)

    const [
        lastCompletedDestinationQuery,
        setLastCompletedDestinationQuery,
    ] = useState<string | null>(null)

    const startRequestId = useRef(0)
    const destinationRequestId = useRef(0)

    const routeCoordinates =
        selectedStart && selectedDestination
            ? {
                start: {
                    label: selectedStart.displayName,
                    latitude: selectedStart.latitude,
                    longitude: selectedStart.longitude,
                },
                destination: {
                    label: selectedDestination.displayName,
                    latitude:
                    selectedDestination.latitude,
                    longitude:
                    selectedDestination.longitude,
                },
            }
            : null

    const departureTimeInFuture =
        isDepartureTimeInFuture(departureTime)

    const departureTimeWithinForecast =
        isDepartureTimeWithinForecastPeriod(
            departureTime,
        )

    const departureTimeIsValid =
        departureTimeInFuture &&
        departureTimeWithinForecast

    const canCalculateRoute =
        selectedProfile !== null &&
        routeCoordinates !== null &&
        departureTimeIsValid

    const [
        searchStartPlaces,
        {
            loading: startLoading,
            error: startError,
        },
    ] = useLazyQuery(SEARCH_PLACES_QUERY)

    const [
        searchDestinationPlaces,
        {
            loading: destinationLoading,
            error: destinationError,
        },
    ] = useLazyQuery(SEARCH_PLACES_QUERY)

    const [
        startRouteCalculation,
        {
            loading: startRouteCalculationLoading,
            error: startRouteCalculationGraphQlError,
        },
    ] = useMutation(
        START_ROUTE_CALCULATION_MUTATION,
    )

    const [
        loadRouteCalculation,
        {
            data: routeCalculationData,
            error: routeCalculationQueryError,
            startPolling,
            stopPolling,
        },
    ] = useLazyQuery(ROUTE_CALCULATION_QUERY, {
        fetchPolicy: 'network-only',
    })

    const stopPollingRef = useRef(stopPolling)
    stopPollingRef.current = stopPolling

    const isPollingRef = useRef(false)

    function stopPollingSafely(): void {
        if (!isPollingRef.current) {
            return
        }

        isPollingRef.current = false

        try {
            stopPollingRef.current?.()
        } catch {
            /*
             * Apollo Client kann einen Fehler auslösen, wenn
             * stopPolling vor der Query-Ausführung aufgerufen wird.
             */
        }
    }

    const routeCalculationIsRunning =
        startRouteCalculationLoading ||
        routeCalculation?.status === 'PENDING' ||
        routeCalculation?.status === 'PROCESSING'

    const completedRouteCalculation =
        routeCalculation?.status === 'COMPLETED'
            ? routeCalculation
            : null

    const routeCanBeExportedAsGpx =
        completedRouteCalculation !== null &&
        canExportRouteAsGpx(
            completedRouteCalculation.geometry,
        )

    const coloredMapSegments = useMemo<RouteMapSegment[]>(() => {
        if (!completedRouteCalculation?.weatherSegments) {
            return []
        }

            return [
                ...completedRouteCalculation.weatherSegments,
            ]
                .sort(
                    (firstSegment, secondSegment) =>
                        firstSegment.segmentIndex -
                        secondSegment.segmentIndex,
                )
                .flatMap((segment) => {
                    const positions = segment.geometry
                        ? toLatLngPositions(
                            segment.geometry.coordinates,
                        )
                        : []

                    return positions.length >= 2
                        ? [
                            {
                                positions,
                                color: segment.color,
                            },
                        ]
                        : []
                })
        }, [completedRouteCalculation])

    useEffect(() => {
        if (selectedProfile) {
            window.localStorage.setItem(
                SELECTED_PROFILE_STORAGE_KEY,
                selectedProfile,
            )

            return
        }

        window.localStorage.removeItem(
            SELECTED_PROFILE_STORAGE_KEY,
        )
    }, [selectedProfile])

    useEffect(() => {
        const trimmedInput = startInput.trim()
        const requestId = ++startRequestId.current

        setLastCompletedStartQuery((currentValue) =>
            currentValue === null ? currentValue : null,
        )

        if (
            trimmedInput.length <
            MINIMUM_SEARCH_LENGTH ||
            selectedStart
        ) {
            setStartResults((currentResults) =>
                currentResults.length === 0
                    ? currentResults
                    : [],
            )

            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchStartPlaces({
                variables: {
                    query: trimmedInput,
                    limit: SEARCH_RESULT_LIMIT,
                },
            }).then((result) => {
                if (
                    !isLatestRequest(
                        requestId,
                        startRequestId.current,
                    )
                ) {
                    return
                }

                setStartResults(
                    (result.data?.searchPlaces ??
                        []) as Place[],
                )

                setLastCompletedStartQuery(
                    trimmedInput,
                )
            })
        }, SEARCH_DEBOUNCE_MS)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [
        startInput,
        selectedStart,
        searchStartPlaces,
    ])

    useEffect(() => {
        const trimmedInput =
            destinationInput.trim()

        const requestId =
            ++destinationRequestId.current

        setLastCompletedDestinationQuery(
            (currentValue) =>
                currentValue === null
                    ? currentValue
                    : null,
        )

        if (
            trimmedInput.length <
            MINIMUM_SEARCH_LENGTH ||
            selectedDestination
        ) {
            setDestinationResults(
                (currentResults) =>
                    currentResults.length === 0
                        ? currentResults
                        : [],
            )

            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchDestinationPlaces({
                variables: {
                    query: trimmedInput,
                    limit: SEARCH_RESULT_LIMIT,
                },
            }).then((result) => {
                if (
                    !isLatestRequest(
                        requestId,
                        destinationRequestId.current,
                    )
                ) {
                    return
                }

                setDestinationResults(
                    (result.data?.searchPlaces ??
                        []) as Place[],
                )

                setLastCompletedDestinationQuery(
                    trimmedInput,
                )
            })
        }, SEARCH_DEBOUNCE_MS)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [
        destinationInput,
        selectedDestination,
        searchDestinationPlaces,
    ])

    useEffect(() => {
        const calculation =
            routeCalculationData?.routeCalculation

        if (
            !calculation ||
            calculation.id !== currentCalculationId
        ) {
            return
        }

        setRouteCalculation(calculation)

        if (
            calculation.status === 'COMPLETED' ||
            calculation.status === 'FAILED'
        ) {
            stopPollingSafely()
        }
    }, [
        currentCalculationId,
        routeCalculationData,
    ])

    useEffect(() => {
        return () => {
            stopPollingSafely()
        }
    }, [])

    useEffect(() => {
        stopPollingSafely()

        setCurrentCalculationId(
            (currentValue) =>
                currentValue === null
                    ? currentValue
                    : null,
        )

        setRouteCalculation(
            (currentValue) =>
                currentValue === null
                    ? currentValue
                    : null,
        )

        setRouteCalculationError(
            (currentValue) =>
                currentValue === null
                    ? currentValue
                    : null,
        )
    }, [
        selectedStart,
        selectedDestination,
        selectedProfile,
        departureTime,
    ])

    function handleStartInputChange(
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        setStartInput(event.target.value)
        setSelectedStart(null)
        setStartResults([])
        setLastCompletedStartQuery(null)
    }

    function handleDestinationInputChange(
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        setDestinationInput(event.target.value)
        setSelectedDestination(null)
        setDestinationResults([])
        setLastCompletedDestinationQuery(null)
    }

    function handleStartSelection(
        place: Place,
    ): void {
        setSelectedStart(place)
        setStartInput(place.displayName)
        setStartResults([])
        setLastCompletedStartQuery(null)
    }

    function handleDestinationSelection(
        place: Place,
    ): void {
        setSelectedDestination(place)
        setDestinationInput(place.displayName)
        setDestinationResults([])
        setLastCompletedDestinationQuery(null)
    }

    function handleDepartureTimeChange(
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        setDepartureTime(event.target.value)

        setMinimumDepartureTime(
            createMinimumDepartureTime(),
        )

        setMaximumDepartureTime(
            createMaximumDepartureTime(),
        )
    }

    function handleGpxExport() {
        if (
            !completedRouteCalculation?.geometry ||
            !routeCanBeExportedAsGpx
        ) {
            return
        }

        downloadRouteAsGpx({
            startLabel:
                completedRouteCalculation.start.label,
            destinationLabel:
                completedRouteCalculation.destination.label,
            geometry:
                completedRouteCalculation.geometry,
        })
    }

    async function handleRouteCalculation() {
        if (!canCalculateRoute || !routeCoordinates || !selectedProfile) {
            return
        }

        stopPollingSafely()

        setRouteCalculationError(null)
        setRouteCalculation(null)

        try {
            const result =
                await startRouteCalculation({
                    variables: {
                        input: {
                            activityProfile:
                            selectedProfile,
                            departureTime:
                                toDepartureTimeIso(
                                    departureTime,
                                ),
                            ...routeCoordinates,
                        },
                    },
                })

            const payload =
                result.data?.startRouteCalculation

            if (!payload) {
                setRouteCalculationError(
                    'Die Routenberechnung konnte nicht gestartet werden.',
                )

                return
            }

            if (payload.errors.length > 0) {
                setRouteCalculationError(
                    payload.errors
                        .map(
                            (error) =>
                                error.message,
                        )
                        .join(' '),
                )

                return
            }

            if (!payload.calculation) {
                setRouteCalculationError(
                    'Die Routenberechnung konnte nicht gestartet werden.',
                )

                return
            }

            const calculationId =
                payload.calculation.id

            setCurrentCalculationId(calculationId)

            setRouteCalculation({
                id: calculationId,
                status: payload.calculation.status,
            } as RouteCalculation)

            await loadRouteCalculation({
                variables: {
                    id: calculationId,
                },
            })

            if (
                payload.calculation.status !==
                'COMPLETED' &&
                payload.calculation.status !==
                'FAILED'
            ) {
                try {
                    isPollingRef.current = true

                    startPolling(
                        ROUTE_CALCULATION_POLL_INTERVAL_MS,
                    )
                } catch {
                    isPollingRef.current = false
                }
            }
        } catch {
            setRouteCalculationError(
                'Die Routenberechnung konnte nicht gestartet werden.',
            )
        }
    }

    return (
        <div className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>
                    Routenplanung
                </p>

                <h1 className={styles.title}>
                    Routenplaner
                </h1>

                <p className={styles.description}>
                    Wähle ein Aktivitätsprofil,
                    Start- und Zielort sowie eine
                    Abfahrtszeit für deine
                    wetteroptimierte Route.
                </p>
            </section>

            <ActivityProfileSelector
                selectedProfile={selectedProfile}
                onSelect={setSelectedProfile}
            />

            <section
                className={styles.card}
                aria-labelledby="route-input-heading"
            >
                <h2
                    id="route-input-heading"
                    style={{margin: '0 0 1rem'}}
                >
                    Route konfigurieren
                </h2>

                <div className={styles.formGrid}>
                    <div className={styles.field}>
                        <label
                            className={styles.label}
                            htmlFor="start-input"
                        >
                            Startpunkt
                        </label>

                        <input
                            id="start-input"
                            name="start"
                            type="text"
                            className={styles.input}
                            value={startInput}
                            onChange={
                                handleStartInputChange
                            }
                            placeholder="Ort oder Adresse eingeben"
                            autoComplete="street-address"
                            maxLength={
                                MAXIMUM_SEARCH_LENGTH
                            }
                            aria-autocomplete="list"
                            aria-controls="start-search-results"
                            aria-expanded={
                                !startLoading &&
                                !startError &&
                                !selectedStart &&
                                startResults.length > 0
                            }
                        />

                        {startLoading && (
                            <p
                                className={styles.hint}
                                role="status"
                            >
                                Startpunkt wird gesucht …
                            </p>
                        )}

                        {startError && (
                            <p
                                className={
                                    styles.fieldError
                                }
                                role="alert"
                            >
                                Der Startpunkt konnte nicht
                                gesucht werden.
                            </p>
                        )}

                        {!startLoading &&
                            !startError &&
                            !selectedStart &&
                            lastCompletedStartQuery ===
                            startInput.trim() &&
                            startResults.length === 0 && (
                                <p
                                    className={
                                        styles.hint
                                    }
                                    role="status"
                                >
                                    Kein passender
                                    Startpunkt gefunden.
                                </p>
                            )}

                        {!startLoading &&
                            !startError &&
                            !selectedStart &&
                            startResults.length > 0 && (
                                <ul
                                    id="start-search-results"
                                    className={
                                        styles.searchResults
                                    }
                                    aria-label="Suchergebnisse für den Startpunkt"
                                >
                                    {startResults.map(
                                        (place) => (
                                            <li
                                                key={
                                                    place.placeId
                                                }
                                                className={
                                                    styles.searchResultItem
                                                }
                                            >
                                                <button
                                                    type="button"
                                                    className={
                                                        styles.searchResultButton
                                                    }
                                                    onClick={() => {
                                                        handleStartSelection(
                                                            place,
                                                        )
                                                    }}
                                                >
                                                    {
                                                        place.displayName
                                                    }
                                                </button>
                                            </li>
                                        ),
                                    )}
                                </ul>
                            )}

                        {selectedStart && (
                            <p
                                className={
                                    styles.selectedStatus
                                }
                                role="status"
                            >
                                <span aria-hidden="true">
                                    ✓
                                </span>{' '}
                                <strong>
                                    Ausgewählt:
                                </strong>{' '}
                                {
                                    selectedStart.displayName
                                }
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label
                            className={styles.label}
                            htmlFor="destination-input"
                        >
                            Zielpunkt
                        </label>

                        <input
                            id="destination-input"
                            name="destination"
                            type="text"
                            className={styles.input}
                            value={destinationInput}
                            onChange={
                                handleDestinationInputChange
                            }
                            placeholder="Ort oder Adresse eingeben"
                            autoComplete="street-address"
                            maxLength={
                                MAXIMUM_SEARCH_LENGTH
                            }
                            aria-autocomplete="list"
                            aria-controls="destination-search-results"
                            aria-expanded={
                                !destinationLoading &&
                                !destinationError &&
                                !selectedDestination &&
                                destinationResults.length >
                                0
                            }
                        />

                        {destinationLoading && (
                            <p
                                className={styles.hint}
                                role="status"
                            >
                                Zielpunkt wird gesucht …
                            </p>
                        )}

                        {destinationError && (
                            <p
                                className={
                                    styles.fieldError
                                }
                                role="alert"
                            >
                                Der Zielpunkt konnte nicht
                                gesucht werden.
                            </p>
                        )}

                        {!destinationLoading &&
                            !destinationError &&
                            !selectedDestination &&
                            lastCompletedDestinationQuery ===
                            destinationInput.trim() &&
                            destinationResults.length ===
                            0 && (
                                <p
                                    className={
                                        styles.hint
                                    }
                                    role="status"
                                >
                                    Kein passender Zielpunkt
                                    gefunden.
                                </p>
                            )}

                        {!destinationLoading &&
                            !destinationError &&
                            !selectedDestination &&
                            destinationResults.length >
                            0 && (
                                <ul
                                    id="destination-search-results"
                                    className={
                                        styles.searchResults
                                    }
                                    aria-label="Suchergebnisse für den Zielpunkt"
                                >
                                    {destinationResults.map(
                                        (place) => (
                                            <li
                                                key={
                                                    place.placeId
                                                }
                                                className={
                                                    styles.searchResultItem
                                                }
                                            >
                                                <button
                                                    type="button"
                                                    className={
                                                        styles.searchResultButton
                                                    }
                                                    onClick={() => {
                                                        handleDestinationSelection(
                                                            place,
                                                        )
                                                    }}
                                                >
                                                    {
                                                        place.displayName
                                                    }
                                                </button>
                                            </li>
                                        ),
                                    )}
                                </ul>
                            )}

                        {selectedDestination && (
                            <p
                                className={
                                    styles.selectedStatus
                                }
                                role="status"
                            >
                                <span aria-hidden="true">
                                    ✓
                                </span>{' '}
                                <strong>
                                    Ausgewählt:
                                </strong>{' '}
                                {
                                    selectedDestination.displayName
                                }
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label
                            className={styles.label}
                            htmlFor="departure-time"
                        >
                            Abfahrtszeit
                        </label>

                        <input
                            id="departure-time"
                            name="departureTime"
                            type="datetime-local"
                            className={styles.input}
                            value={departureTime}
                            min={minimumDepartureTime}
                            max={maximumDepartureTime}
                            step={15 * 60}
                            onChange={
                                handleDepartureTimeChange
                            }
                            required
                            aria-invalid={
                                !departureTimeIsValid
                            }
                            aria-describedby="departure-time-hint"
                        />

                        <p
                            id="departure-time-hint"
                            className={styles.hint}
                        >
                            Wähle eine zukünftige
                            Abfahrtszeit im
                            Vorhersagebereich. Möglich sind
                            bis zu sieben Tage im Voraus in
                            15-Minuten-Schritten.
                        </p>

                        {!departureTimeInFuture && (
                            <p
                                className={
                                    styles.fieldError
                                }
                                role="alert"
                            >
                                Die Abfahrtszeit muss in
                                der Zukunft liegen.
                            </p>
                        )}

                        {departureTimeInFuture &&
                            !departureTimeWithinForecast && (
                                <p
                                    className={
                                        styles.fieldError
                                    }
                                    role="alert"
                                >
                                    Die Abfahrtszeit darf
                                    maximal sieben Tage in
                                    der Zukunft liegen.
                                </p>
                            )}
                    </div>

                    <button
                        type="button"
                        className={
                            styles.calculateButton
                        }
                        disabled={
                            !canCalculateRoute ||
                            routeCalculationIsRunning
                        }
                        aria-busy={
                            routeCalculationIsRunning
                        }
                        onClick={() => {
                            void handleRouteCalculation()
                        }}
                    >
                        {routeCalculationIsRunning
                            ? 'Route wird berechnet …'
                            : 'Route berechnen'}
                    </button>
                </div>

                {startRouteCalculationLoading && (
                    <div
                        className={[
                            styles.statusBanner,
                            styles.statusBannerInfo,
                        ].join(' ')}
                        role="status"
                        aria-live="polite"
                    >
                        <span aria-hidden="true">
                            ⏳
                        </span>

                        <span>
                            Die Routenberechnung wird
                            gestartet.
                        </span>
                    </div>
                )}

                {!startRouteCalculationLoading &&
                    routeCalculation?.status ===
                    'PENDING' && (
                        <div
                            className={[
                                styles.statusBanner,
                                styles.statusBannerInfo,
                            ].join(' ')}
                            role="status"
                            aria-live="polite"
                        >
                            <span aria-hidden="true">
                                🕒
                            </span>

                            <span>
                                Die Routenberechnung wartet
                                auf die Verarbeitung.
                            </span>
                        </div>
                    )}

                {!startRouteCalculationLoading &&
                    routeCalculation?.status ===
                    'PROCESSING' && (
                        <div
                            className={[
                                styles.statusBanner,
                                styles.statusBannerInfo,
                            ].join(' ')}
                            role="status"
                            aria-live="polite"
                        >
                            <span aria-hidden="true">
                                ⚙️
                            </span>

                            <span>
                                Die Route wird aktuell
                                verarbeitet.
                            </span>
                        </div>
                    )}

                {routeCalculation?.status ===
                    'COMPLETED' && (
                        <div
                            className={[
                                styles.statusBanner,
                                styles.statusBannerSuccess,
                            ].join(' ')}
                            role="status"
                        >
                        <span aria-hidden="true">
                            ✅
                        </span>

                            <span>
                            Die Routenberechnung wurde
                            erfolgreich abgeschlossen.
                        </span>
                        </div>
                    )}

                {routeCalculation?.status ===
                    'FAILED' && (
                        <div
                            className={[
                                styles.statusBanner,
                                styles.statusBannerError,
                            ].join(' ')}
                            role="alert"
                        >
                        <span aria-hidden="true">
                            ⚠️
                        </span>

                            <span>
                            {routeCalculation.error
                                    ?.message ??
                                'Für diese Anfrage konnte keine Route berechnet werden.'}
                        </span>
                        </div>
                    )}

                {routeCalculationError && (
                    <div
                        className={[
                            styles.statusBanner,
                            styles.statusBannerError,
                        ].join(' ')}
                        role="alert"
                    >
                        <span aria-hidden="true">
                            ⚠️
                        </span>

                        <span>
                            {routeCalculationError}
                        </span>
                    </div>
                )}

                {startRouteCalculationGraphQlError && (
                    <div
                        className={[
                            styles.statusBanner,
                            styles.statusBannerError,
                        ].join(' ')}
                        role="alert"
                    >
                        <span aria-hidden="true">
                            ⚠️
                        </span>

                        <span>
                            Die Routenberechnung konnte
                            wegen eines
                            Kommunikationsfehlers nicht
                            gestartet werden.
                        </span>
                    </div>
                )}

                {routeCalculationQueryError && (
                    <div
                        className={[
                            styles.statusBanner,
                            styles.statusBannerError,
                        ].join(' ')}
                        role="alert"
                    >
                        <span aria-hidden="true">
                            ⚠️
                        </span>

                        <span>
                            Der aktuelle Stand der
                            Routenberechnung konnte nicht
                            abgerufen werden.
                        </span>
                    </div>
                )}
            </section>

            {completedRouteCalculation && (
                <section
                    className={styles.card}
                    aria-labelledby="route-result-heading"
                >
                    <div className={styles.resultsHeader}>
                        <h2 id="route-result-heading">Routenergebnis</h2>

                        <button
                            type="button"
                            className={styles.exportButton}
                            disabled={!routeCanBeExportedAsGpx}
                            onClick={handleGpxExport}
                        >
                            Als GPX exportieren
                        </button>
                    </div>

                    <dl className={styles.statsGrid}>
                        <div className={styles.statCard}>
                            <dt
                                className={
                                    styles.statLabel
                                }
                            >
                                Distanz
                            </dt>

                            <dd
                                className={
                                    styles.statValue
                                }
                            >
                                {completedRouteCalculation.distanceMeters !==
                                null
                                    ? formatDistance(
                                        completedRouteCalculation.distanceMeters,
                                    )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>

                        <div className={styles.statCard}>
                            <dt
                                className={
                                    styles.statLabel
                                }
                            >
                                Fahrtdauer
                            </dt>

                            <dd
                                className={
                                    styles.statValue
                                }
                            >
                                {completedRouteCalculation.durationSeconds !==
                                null
                                    ? formatDuration(
                                        completedRouteCalculation.durationSeconds,
                                    )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>

                        <div className={styles.statCard}>
                            <dt
                                className={
                                    styles.statLabel
                                }
                            >
                                Ankunftszeit
                            </dt>

                            <dd
                                className={
                                    styles.statValue
                                }
                            >
                                {completedRouteCalculation.arrivalTime
                                    ? formatArrivalTime(
                                        completedRouteCalculation.arrivalTime,
                                    )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>

                        {typeof completedRouteCalculation.weatherScore ===
                            'number' && (
                                <div
                                    className={
                                        styles.statCard
                                    }
                                >
                                    <dt
                                        className={
                                            styles.statLabel
                                        }
                                    >
                                        Wetterscore
                                    </dt>

                                    <dd
                                        className={
                                            styles.statValue
                                        }
                                    >
                                        {
                                            completedRouteCalculation.weatherScore
                                        }{' '}
                                        / 100
                                    </dd>
                                </div>
                            )}
                    </dl>

                    {completedRouteCalculation.thunderstormCapApplied && (
                        <div
                            className={[
                                styles.statusBanner,
                                styles.statusBannerError,
                            ].join(' ')}
                            role="status"
                            style={{
                                marginTop: '0.75rem',
                            }}
                        >
                            <span aria-hidden="true">
                                ⚡
                            </span>

                            <span>
                                Erhöhtes Gewitterrisiko
                                auf der Route erkannt. Der
                                Gesamtwetterscore wurde
                                auf maximal 50 begrenzt.
                            </span>
                        </div>
                    )}
                </section>
            )}

            <section
                className={styles.mapWrapper}
                aria-label="Kartendarstellung der Route"
            >
                <RouteMap
                    startPlace={selectedStart}
                    destinationPlace={
                        selectedDestination
                    }
                    routeSegments={
                        coloredMapSegments
                    }
                    geometry={
                        completedRouteCalculation
                            ?.geometry ?? null
                    }
                />
            </section>

            {completedRouteCalculation && (
                <section
                    className={styles.card}
                    aria-labelledby="save-route-heading"
                >
                    <h2
                        id="save-route-heading"
                        style={{margin: '0 0 1rem'}}
                    >
                        Route speichern
                    </h2>

                    {isAuthenticated ? (
                        <SaveRouteForm
                            routeCalculationId={
                                completedRouteCalculation.id
                            }
                            weatherEvaluationId={(
                                completedRouteCalculation.weatherEvaluationId ??
                                `w_${completedRouteCalculation.id}`
                            ).slice(0, 40)}
                        />
                    ) : (
                        <p className={styles.hint}>
                            Bitte{' '}
                            <Link
                                to="/login"
                                style={{
                                    color: 'var(--color-primary)',
                                    fontWeight: 700,
                                }}
                            >
                                melde dich an
                            </Link>
                            , um die Route zu speichern.
                        </p>
                    )}
                </section>
            )}
        </div>
    )
}