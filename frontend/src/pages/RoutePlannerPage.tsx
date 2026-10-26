import {
    useEffect,
    useRef,
    useState,
    type ChangeEvent,
} from 'react'
import {
    useLazyQuery,
    useMutation,
} from '@apollo/client/react'
import { Link } from 'react-router'

import { useAuth } from '../features/auth/useAuth'
import SaveRouteForm from '../features/saved-routes/SaveRouteForm'
import RouteMap from '../components/RouteMap/RouteMap'
import ActivityProfileSelector from '../features/activity-profiles/ActivityProfileSelector'
import type { ActivityProfileCode } from '../types/activityProfile'
import type { Place } from '../types/searchPlaces'
import type { RouteCalculation } from '../types/routeCalculation'
import { SEARCH_PLACES_QUERY } from '../api/searchPlacesQuery'
import {
    ROUTE_CALCULATION_QUERY,
    START_ROUTE_CALCULATION_MUTATION,
} from '../api/routeCalculationApi'
import { isLatestRequest } from '../utils/latestRequest'
import {
    createInitialDepartureTime,
    createMinimumDepartureTime,
    createMaximumDepartureTime,
    isDepartureTimeInFuture,
    isDepartureTimeWithinForecastPeriod,
    toDepartureTimeIso,
} from '../features/route-planner/departureTime'
import {
    formatArrivalTime,
    formatDistance,
    formatDuration,
} from '../features/route-planner/routeResultFormatting'

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
    const { isAuthenticated } = useAuth()

    const [selectedProfile, setSelectedProfile] =
        useState<ActivityProfileCode | null>(readSelectedProfile)

    const [startInput, setStartInput] = useState('')
    const [destinationInput, setDestinationInput] = useState('')

    const [departureTime, setDepartureTime] = useState(
        createInitialDepartureTime,
    )
    const [minimumDepartureTime, setMinimumDepartureTime] =
        useState(createMinimumDepartureTime)
    const [maximumDepartureTime, setMaximumDepartureTime] =
        useState(createMaximumDepartureTime)

    const [currentCalculationId, setCurrentCalculationId] =
        useState<string | null>(null)
    const [routeCalculation, setRouteCalculation] =
        useState<RouteCalculation | null>(null)
    const [routeCalculationError, setRouteCalculationError] =
        useState<string | null>(null)

    const [selectedStart, setSelectedStart] =
        useState<Place | null>(null)
    const [selectedDestination, setSelectedDestination] =
        useState<Place | null>(null)

    const [startResults, setStartResults] = useState<Place[]>([])
    const [destinationResults, setDestinationResults] =
        useState<Place[]>([])

    const [lastCompletedStartQuery, setLastCompletedStartQuery] =
        useState<string | null>(null)
    const [lastCompletedDestinationQuery, setLastCompletedDestinationQuery] =
        useState<string | null>(null)

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
                      latitude: selectedDestination.latitude,
                      longitude: selectedDestination.longitude,
                  },
              }
            : null

    const departureTimeInFuture = isDepartureTimeInFuture(departureTime)
    const departureTimeWithinForecast = isDepartureTimeWithinForecastPeriod(departureTime)
    const departureTimeIsValid = departureTimeInFuture && departureTimeWithinForecast

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
    ] = useMutation(START_ROUTE_CALCULATION_MUTATION)

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

    function stopPollingSafely() {
        if (!isPollingRef.current) {
            return
        }
        isPollingRef.current = false
        try {
            stopPollingRef.current?.()
        } catch {
            // Apollo Client wirft einen Fehler, wenn stopPolling vor Ausführung der Query aufgerufen wird
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

        setLastCompletedStartQuery((prev) => (prev === null ? prev : null))

        if (
            trimmedInput.length < MINIMUM_SEARCH_LENGTH ||
            selectedStart
        ) {
            setStartResults((prev) => (prev.length === 0 ? prev : []))
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
                    (result.data?.searchPlaces ?? []) as Place[],
                )
                setLastCompletedStartQuery(trimmedInput)
            })
        }, SEARCH_DEBOUNCE_MS)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [startInput, selectedStart, searchStartPlaces])

    useEffect(() => {
        const trimmedInput = destinationInput.trim()
        const requestId = ++destinationRequestId.current

        setLastCompletedDestinationQuery((prev) => (prev === null ? prev : null))

        if (
            trimmedInput.length < MINIMUM_SEARCH_LENGTH ||
            selectedDestination
        ) {
            setDestinationResults((prev) => (prev.length === 0 ? prev : []))
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
                    (result.data?.searchPlaces ?? []) as Place[],
                )
                setLastCompletedDestinationQuery(trimmedInput)
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
        const calculation = routeCalculationData?.routeCalculation

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

    // Wenn Eingaben geändert werden, vorherige Berechnung und Geometrie zurücksetzen
    useEffect(() => {
        stopPollingSafely()
        setCurrentCalculationId((prev) => (prev === null ? prev : null))
        setRouteCalculation((prev) => (prev === null ? prev : null))
        setRouteCalculationError((prev) => (prev === null ? prev : null))
    }, [
        selectedStart,
        selectedDestination,
        selectedProfile,
        departureTime,
    ])

    function handleStartInputChange(event: ChangeEvent<HTMLInputElement>) {
        setStartInput(event.target.value)
        setSelectedStart(null)
        setStartResults([])
        setLastCompletedStartQuery(null)
    }

    function handleDestinationInputChange(event: ChangeEvent<HTMLInputElement>) {
        setDestinationInput(event.target.value)
        setSelectedDestination(null)
        setDestinationResults([])
        setLastCompletedDestinationQuery(null)
    }

    function handleStartSelection(place: Place) {
        setSelectedStart(place)
        setStartInput(place.displayName)
        setStartResults([])
        setLastCompletedStartQuery(null)
    }

    function handleDestinationSelection(place: Place) {
        setSelectedDestination(place)
        setDestinationInput(place.displayName)
        setDestinationResults([])
        setLastCompletedDestinationQuery(null)
    }

    function handleDepartureTimeChange(event: ChangeEvent<HTMLInputElement>) {
        setDepartureTime(event.target.value)
        setMinimumDepartureTime(createMinimumDepartureTime())
        setMaximumDepartureTime(createMaximumDepartureTime())
    }

    async function handleRouteCalculation() {
        if (!canCalculateRoute || !routeCoordinates || !selectedProfile) {
            return
        }

        stopPollingSafely()
        setRouteCalculationError(null)
        setRouteCalculation(null)

        try {
            const result = await startRouteCalculation({
                variables: {
                    input: {
                        activityProfile: selectedProfile,
                        departureTime: toDepartureTimeIso(departureTime),
                        ...routeCoordinates,
                    },
                },
            })

            const payload = result?.data?.startRouteCalculation

            if (!payload) {
                setRouteCalculationError(
                    'Die Routenberechnung konnte nicht gestartet werden.',
                )
                return
            }

            if (payload.errors.length > 0) {
                setRouteCalculationError(
                    payload.errors
                        .map((error) => error.message)
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

            const calculationId = payload.calculation.id

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

            if (payload.calculation.status !== 'COMPLETED' && payload.calculation.status !== 'FAILED') {
                try {
                    isPollingRef.current = true
                    startPolling(ROUTE_CALCULATION_POLL_INTERVAL_MS)
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
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Routenplanung</p>
                <h1 className={styles.title}>Routenplaner</h1>
                <p className={styles.description}>
                    Wähle ein Aktivitätsprofil, Start- und Zielort sowie eine
                    Abfahrtszeit für deine wetteroptimierte Route.
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
                <h2 id="route-input-heading" style={{ margin: '0 0 1rem' }}>
                    Route konfigurieren
                </h2>

                <div className={styles.formGrid}>
                    <div className={styles.field}>
                        <label className={styles.label} htmlFor="start-input">
                            Startpunkt
                        </label>
                        <input
                            id="start-input"
                            name="start"
                            type="text"
                            className={styles.input}
                            value={startInput}
                            onChange={handleStartInputChange}
                            placeholder="Ort oder Adresse eingeben"
                            autoComplete="street-address"
                            maxLength={MAXIMUM_SEARCH_LENGTH}
                        />

                        {startLoading && (
                            <p className={styles.hint} aria-live="polite">
                                Startpunkt wird gesucht...
                            </p>
                        )}

                        {startError && (
                            <p className={styles.fieldError} role="alert">
                                Der Startpunkt konnte nicht gesucht werden.
                            </p>
                        )}

                        {!startLoading &&
                            !startError &&
                            !selectedStart &&
                            lastCompletedStartQuery === startInput.trim() &&
                            startResults.length === 0 && (
                                <p className={styles.hint} role="status">
                                    Kein passender Startpunkt gefunden.
                                </p>
                            )}

                        {!startLoading &&
                            !startError &&
                            !selectedStart &&
                            startResults.length > 0 && (
                                <ul
                                    className={styles.searchResults}
                                    aria-label="Suchergebnisse für den Startpunkt"
                                >
                                    {startResults.map((place) => (
                                        <li
                                            key={place.placeId}
                                            className={styles.searchResultItem}
                                        >
                                            <button
                                                type="button"
                                                className={styles.searchResultButton}
                                                onClick={() =>
                                                    handleStartSelection(place)
                                                }
                                            >
                                                {place.displayName}
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}

                        {selectedStart && (
                            <p className={styles.selectedStatus} role="status">
                                <span>✓</span>
                                <strong>Ausgewählt:</strong> {selectedStart.displayName}
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label className={styles.label} htmlFor="destination-input">
                            Zielpunkt
                        </label>
                        <input
                            id="destination-input"
                            name="destination"
                            type="text"
                            className={styles.input}
                            value={destinationInput}
                            onChange={handleDestinationInputChange}
                            placeholder="Ort oder Adresse eingeben"
                            autoComplete="street-address"
                            maxLength={MAXIMUM_SEARCH_LENGTH}
                        />

                        {destinationLoading && (
                            <p className={styles.hint} aria-live="polite">
                                Zielpunkt wird gesucht...
                            </p>
                        )}

                        {destinationError && (
                            <p className={styles.fieldError} role="alert">
                                Der Zielpunkt konnte nicht gesucht werden.
                            </p>
                        )}

                        {!destinationLoading &&
                            !destinationError &&
                            !selectedDestination &&
                            lastCompletedDestinationQuery === destinationInput.trim() &&
                            destinationResults.length === 0 && (
                                <p className={styles.hint} role="status">
                                    Kein passender Zielpunkt gefunden.
                                </p>
                            )}

                        {!destinationLoading &&
                            !destinationError &&
                            !selectedDestination &&
                            destinationResults.length > 0 && (
                                <ul
                                    className={styles.searchResults}
                                    aria-label="Suchergebnisse für den Zielpunkt"
                                >
                                    {destinationResults.map((place) => (
                                        <li
                                            key={place.placeId}
                                            className={styles.searchResultItem}
                                        >
                                            <button
                                                type="button"
                                                className={styles.searchResultButton}
                                                onClick={() =>
                                                    handleDestinationSelection(place)
                                                }
                                            >
                                                {place.displayName}
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}

                        {selectedDestination && (
                            <p className={styles.selectedStatus} role="status">
                                <span>✓</span>
                                <strong>Ausgewählt:</strong> {selectedDestination.displayName}
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label className={styles.label} htmlFor="departure-time">
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
                            onChange={handleDepartureTimeChange}
                            required
                            aria-describedby="departure-time-hint"
                        />
                        <p id="departure-time-hint" className={styles.hint}>
                            Wähle eine zukünftige Abfahrtszeit im Vorhersagebereich (bis zu 7 Tage im Voraus, in 15-Minuten-Schritten).
                        </p>
                        {!departureTimeInFuture && (
                            <p className={styles.fieldError} role="alert">
                                Die Abfahrtszeit muss in der Zukunft liegen.
                            </p>
                        )}
                        {departureTimeInFuture && !departureTimeWithinForecast && (
                            <p className={styles.fieldError} role="alert">
                                Die Abfahrtszeit darf maximal 7 Tage in der Zukunft liegen (Vorhersagezeitraum).
                            </p>
                        )}
                    </div>

                    <button
                        type="button"
                        className={styles.calculateButton}
                        disabled={!canCalculateRoute || routeCalculationIsRunning}
                        aria-busy={routeCalculationIsRunning}
                        onClick={() => {
                            void handleRouteCalculation()
                        }}
                    >
                        {routeCalculationIsRunning
                            ? 'Route wird berechnet …'
                            : 'Route berechnen'}
                    </button>
                </div>

                {routeCalculationIsRunning && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerInfo}`} role="status" aria-live="polite">
                        <span>⏳</span>
                        <span>Die Route wird berechnet. Bitte warte einen Moment.</span>
                    </div>
                )}

                {routeCalculation?.status === 'PENDING' && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerInfo}`} role="status">
                        <span>🕒</span>
                        <span>Die Routenberechnung wartet auf die Verarbeitung.</span>
                    </div>
                )}

                {routeCalculation?.status === 'PROCESSING' && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerInfo}`} role="status">
                        <span>⚙️</span>
                        <span>Die Route wird aktuell verarbeitet.</span>
                    </div>
                )}

                {routeCalculation?.status === 'COMPLETED' && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerSuccess}`} role="status">
                        <span>✅</span>
                        <span>Die Routenberechnung wurde erfolgreich abgeschlossen.</span>
                    </div>
                )}

                {routeCalculation?.status === 'FAILED' && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerError}`} role="alert">
                        <span>⚠️</span>
                        <span>
                            {routeCalculation.error?.message ??
                                'Für diese Anfrage konnte keine Route berechnet werden.'}
                        </span>
                    </div>
                )}

                {routeCalculationError && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerError}`} role="alert">
                        <span>⚠️</span>
                        <span>{routeCalculationError}</span>
                    </div>
                )}

                {startRouteCalculationGraphQlError && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerError}`} role="alert">
                        <span>⚠️</span>
                        <span>
                            Die Routenberechnung konnte wegen eines
                            Kommunikationsfehlers nicht gestartet werden.
                        </span>
                    </div>
                )}

                {routeCalculationQueryError && (
                    <div className={`${styles.statusBanner} ${styles.statusBannerError}`} role="alert">
                        <span>⚠️</span>
                        <span>
                            Der aktuelle Stand der Routenberechnung konnte
                            nicht abgerufen werden.
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
                    </div>

                    <dl className={styles.statsGrid}>
                        <div className={styles.statCard}>
                            <dt className={styles.statLabel}>Distanz</dt>
                            <dd className={styles.statValue}>
                                {completedRouteCalculation.distanceMeters !== null
                                    ? formatDistance(
                                          completedRouteCalculation.distanceMeters,
                                      )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>

                        <div className={styles.statCard}>
                            <dt className={styles.statLabel}>Fahrtdauer</dt>
                            <dd className={styles.statValue}>
                                {completedRouteCalculation.durationSeconds !== null
                                    ? formatDuration(
                                          completedRouteCalculation.durationSeconds,
                                      )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>

                        <div className={styles.statCard}>
                            <dt className={styles.statLabel}>Ankunftszeit</dt>
                            <dd className={styles.statValue}>
                                {completedRouteCalculation.arrivalTime
                                    ? formatArrivalTime(
                                          completedRouteCalculation.arrivalTime,
                                      )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>
                    </dl>
                </section>
            )}

            <div className={styles.mapWrapper}>
                <RouteMap
                    startPlace={selectedStart}
                    destinationPlace={selectedDestination}
                    geometry={completedRouteCalculation?.geometry ?? null}
                />
            </div>

            {completedRouteCalculation && (
                <section
                    className={styles.card}
                    aria-labelledby="save-route-heading"
                >
                    <h2 id="save-route-heading" style={{ margin: '0 0 1rem' }}>
                        Route speichern
                    </h2>

                    {isAuthenticated ? (
                        <SaveRouteForm
                            routeCalculationId={completedRouteCalculation.id}
                            weatherEvaluationId={`weather_${completedRouteCalculation.id}`}
                        />
                    ) : (
                        <p className={styles.hint}>
                            Bitte <Link to="/login" style={{ color: 'var(--color-primary, #2563eb)', fontWeight: 700 }}>melde dich an</Link>, um
                            die Route zu speichern.
                        </p>
                    )}
                </section>
            )}
        </main>
    )
}
