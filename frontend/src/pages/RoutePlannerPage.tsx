// [KI-Log Nr. KI-15 & KI-19] KI-generiert und optimiert (Zusammenführung KI-Analyse, GPX-Export & Polling-Stabilität)
import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
    type ChangeEvent,
} from 'react'
import { useLazyQuery, useMutation } from '@apollo/client/react'
import { Link, useLocation } from 'react-router'

import type { WeatherOptimizationResponse } from '../types/weatherOptimization'
import type { RouteWeatherAiAnalysisResponse } from '../types/weatherAiAnalysis'
import { fetchWeatherOptimization } from '../api/weatherOptimizationApi'
import { fetchRouteWeatherAiAnalysis } from '../api/weatherAiAnalysisApi'
import { ApiRequestError } from '../api/restClient'
import { getScoreColor } from '../utils/weatherColorScale'
import { SEARCH_PLACES_QUERY } from '../api/searchPlacesQuery'
import {
    ROUTE_CALCULATION_QUERY,
    START_ROUTE_CALCULATION_MUTATION,
} from '../api/routeCalculationApi'
import AiWeatherAdvisorPanel from '../components/AiWeatherAdvisor/AiWeatherAdvisorPanel'
import MeteogramDock from '../components/MeteogramDock/MeteogramDock'
import RouteMap from '../components/RouteMap/RouteMap'
import DepartureTimeSlider from '../components/DepartureTimePicker/DepartureTimeSlider'
import ActivityProfileSelector from '../features/activity-profiles/ActivityProfileSelector'
import { useAuth } from '../features/auth/useAuth'
import {
    createInitialDepartureTime,
    createMaximumDepartureTime,
    createMinimumDepartureTime,
    isDepartureTimeInFuture,
    isDepartureTimeWithinForecastPeriod,
    toDepartureTimeIso,
} from '../features/route-planner/departureTime'
import {
    canExportRouteAsGpx,
    downloadRouteAsGpx,
} from '../features/route-planner/gpxExport'
import {
    formatArrivalTime,
    formatDistance,
    formatDuration,
} from '../features/route-planner/routeResultFormatting'
import SaveRouteForm from '../features/saved-routes/SaveRouteForm'
import { toLatLngPositions } from '../features/saved-routes/routeGeometry'
import type { ActivityProfileCode } from '../types/activityProfile'
import type {
    RouteCalculation,
    RouteWeatherSegment,
} from '../types/routeCalculation'
import type { RouteMapSegment } from '../types/routeMap'
import type { Place } from '../types/searchPlaces'
import { isLatestRequest } from '../utils/latestRequest'

import styles from './RoutePlannerPage.module.css'

const SELECTED_PROFILE_STORAGE_KEY = 'warp-selected-activity-profile'
const MINIMUM_SEARCH_LENGTH = 3
const MAXIMUM_SEARCH_LENGTH = 200
const SEARCH_RESULT_LIMIT = 5
const SEARCH_DEBOUNCE_MS = 400
const ROUTE_CALCULATION_POLL_INTERVAL_MS = 1000

function readSelectedProfile(): ActivityProfileCode | null {
    if (typeof window === 'undefined') {
        return null
    }

    const value = window.localStorage.getItem(SELECTED_PROFILE_STORAGE_KEY)

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
    const location = useLocation()

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
    const [optimizationData, setOptimizationData] =
        useState<WeatherOptimizationResponse | null>(null)
    const [selectedSlotIndex, setSelectedSlotIndex] =
        useState<number | null>(null)
    const [userSelectedDepartureIso, setUserSelectedDepartureIso] =
        useState<string | null>(null)
    const [hoveredSegmentIndex, setHoveredSegmentIndex] =
        useState<number | null>(null)
    const [aiAnalysis, setAiAnalysis] =
        useState<RouteWeatherAiAnalysisResponse | null>(null)
    const [aiAnalysisLoading, setAiAnalysisLoading] = useState(false)
    const [aiAnalysisError, setAiAnalysisError] =
        useState<string | null>(null)
    const [aiAnalysisErrorCode, setAiAnalysisErrorCode] =
        useState<string | null>(null)
    const [aiRefreshTrigger, setAiRefreshTrigger] = useState(0)
    const [selectedStart, setSelectedStart] = useState<Place | null>(null)
    const [selectedDestination, setSelectedDestination] =
        useState<Place | null>(null)
    const [startResults, setStartResults] = useState<Place[]>([])
    const [destinationResults, setDestinationResults] = useState<Place[]>([])
    const [lastCompletedStartQuery, setLastCompletedStartQuery] =
        useState<string | null>(null)
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
                    latitude: selectedDestination.latitude,
                    longitude: selectedDestination.longitude,
                },
            }
            : null

    const departureTimeInFuture = isDepartureTimeInFuture(departureTime)
    const departureTimeWithinForecast =
        isDepartureTimeWithinForecastPeriod(departureTime)
    const departureTimeIsValid =
        departureTimeInFuture && departureTimeWithinForecast
    const canCalculateRoute =
        selectedProfile !== null &&
        routeCoordinates !== null &&
        departureTimeIsValid

    const [
        searchStartPlaces,
        { loading: startLoading, error: startError },
    ] = useLazyQuery(SEARCH_PLACES_QUERY)
    const [
        searchDestinationPlaces,
        { loading: destinationLoading, error: destinationError },
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

    function stopPollingSafely(): void {
        if (!isPollingRef.current) {
            return
        }

        isPollingRef.current = false

        try {
            stopPollingRef.current?.()
        } catch {
            /* Apollo kann einen Fehler werfen, wenn polling vor der Ausführung gestoppt wird */
        }
    }

    const routeCalculationIsRunning =
        startRouteCalculationLoading ||
        routeCalculation?.status === 'PENDING' ||
        routeCalculation?.status === 'PROCESSING'
    const completedRouteCalculation =
        routeCalculation?.status === 'COMPLETED' ? routeCalculation : null
    const routeCanBeExportedAsGpx =
        completedRouteCalculation !== null &&
        canExportRouteAsGpx(completedRouteCalculation.geometry)

    useEffect(() => {
        const replan = (
            location.state as {
                replanRoute?: {
                    start?: Place
                    destination?: Place
                    activityProfile?: ActivityProfileCode
                }
            } | null
        )?.replanRoute

        if (replan) {
            if (replan.start) {
                setSelectedStart(replan.start)
                setStartInput(replan.start.displayName)
            }
            if (replan.destination) {
                setSelectedDestination(replan.destination)
                setDestinationInput(replan.destination.displayName)
            }
            if (replan.activityProfile) {
                setSelectedProfile(replan.activityProfile)
            }
            setDepartureTime(createInitialDepartureTime())
        }
    }, [location.state])

    useEffect(() => {
        if (
            !completedRouteCalculation?.geometry?.coordinates ||
            completedRouteCalculation.geometry.coordinates.length < 2
        ) {
            setOptimizationData(null)
            setSelectedSlotIndex(null)
            setUserSelectedDepartureIso(null)
            return
        }

        let isCancelled = false
        fetchWeatherOptimization({
            routeCalculationId: completedRouteCalculation.id,
            activityProfile:
                selectedProfile ?? completedRouteCalculation.activityProfile?.code,
            departureTime: completedRouteCalculation.departureTime,
            durationSeconds:
                completedRouteCalculation.durationSeconds ?? undefined,
            distanceMeters:
                completedRouteCalculation.distanceMeters ?? undefined,
            coordinates: completedRouteCalculation.geometry
                .coordinates as [number, number][],
        })
            .then((data) => {
                if (isCancelled) return
                setOptimizationData(data)
                // Select initial departure time slot so route score and map stay consistent without jumping
                const targetMs = new Date(
                    completedRouteCalculation.departureTime,
                ).getTime()
                let initialIdx = 0
                let minDiff = Infinity
                data.departureSlots.forEach((slot, idx) => {
                    const diff = Math.abs(
                        new Date(slot.departureIso).getTime() - targetMs,
                    )
                    if (diff < minDiff) {
                        minDiff = diff
                        initialIdx = idx
                    }
                })
                setSelectedSlotIndex(initialIdx)
            })
            .catch(() => {
                if (isCancelled) return
                setOptimizationData(null)
            })

        return () => {
            isCancelled = true
        }
    }, [
        completedRouteCalculation?.id,
        completedRouteCalculation?.status,
        completedRouteCalculation?.geometry,
        completedRouteCalculation?.departureTime,
        completedRouteCalculation?.durationSeconds,
        completedRouteCalculation?.distanceMeters,
        selectedProfile,
        completedRouteCalculation?.activityProfile?.code,
    ])

    const initialDepartureSlotIndex = useMemo(() => {
        if (
            !optimizationData?.departureSlots ||
            !completedRouteCalculation?.departureTime
        ) {
            return 0
        }
        const targetMs = new Date(
            completedRouteCalculation.departureTime,
        ).getTime()
        let closestIdx = 0
        let minDiff = Infinity
        optimizationData.departureSlots.forEach((slot, idx) => {
            const diff = Math.abs(
                new Date(slot.departureIso).getTime() - targetMs,
            )
            if (diff < minDiff) {
                minDiff = diff
                closestIdx = idx
            }
        })
        return closestIdx
    }, [optimizationData, completedRouteCalculation?.departureTime])

    const activeSlot = useMemo(() => {
        if (
            !optimizationData?.departureSlots ||
            selectedSlotIndex === null
        ) {
            return null
        }
        return optimizationData.departureSlots[selectedSlotIndex] ?? null
    }, [optimizationData, selectedSlotIndex])

    const handleRefreshAiAnalysis = useCallback(() => {
        setAiRefreshTrigger((prev) => prev + 1)
    }, [])

    useEffect(() => {
        if (
            !isAuthenticated ||
            !completedRouteCalculation?.geometry?.coordinates ||
            completedRouteCalculation.geometry.coordinates.length < 2
        ) {
            setAiAnalysis(null)
            setAiAnalysisLoading(false)
            setAiAnalysisError(null)
            setAiAnalysisErrorCode(null)
            return
        }

        let isCancelled = false
        setAiAnalysisLoading(true)
        setAiAnalysisError(null)
        setAiAnalysisErrorCode(null)

        fetchRouteWeatherAiAnalysis({
            routeCalculationId: completedRouteCalculation.id,
            startLabel: completedRouteCalculation.start?.label,
            destinationLabel: completedRouteCalculation.destination?.label,
            activityProfile:
                selectedProfile ??
                completedRouteCalculation.activityProfile?.code,
            departureTime:
                userSelectedDepartureIso ??
                completedRouteCalculation.departureTime,
            durationSeconds:
                completedRouteCalculation.durationSeconds ?? undefined,
            distanceMeters:
                completedRouteCalculation.distanceMeters ?? undefined,
            coordinates: completedRouteCalculation.geometry
                .coordinates as [number, number][],
        })
            .then((data) => {
                if (isCancelled) return
                setAiAnalysis(data)
                setAiAnalysisLoading(false)
            })
            .catch((err: unknown) => {
                if (isCancelled) return
                setAiAnalysisLoading(false)
                if (err instanceof ApiRequestError) {
                    setAiAnalysisError(err.message)
                    setAiAnalysisErrorCode(err.code ?? null)
                } else {
                    setAiAnalysisError(
                        'Die KI-Wetteranalyse konnte momentan nicht geladen werden.',
                    )
                    setAiAnalysisErrorCode(null)
                }
            })

        return () => {
            isCancelled = true
        }
    }, [
        isAuthenticated,
        completedRouteCalculation?.id,
        completedRouteCalculation?.status,
        completedRouteCalculation?.geometry,
        completedRouteCalculation?.departureTime,
        completedRouteCalculation?.durationSeconds,
        completedRouteCalculation?.distanceMeters,
        completedRouteCalculation?.start?.label,
        completedRouteCalculation?.destination?.label,
        completedRouteCalculation?.activityProfile?.code,
        selectedProfile,
        userSelectedDepartureIso,
        aiRefreshTrigger,
    ])

    const activeWeatherSegments = useMemo<
        RouteWeatherSegment[] | undefined
    >(() => {
        if (!completedRouteCalculation?.weatherSegments) {
            return undefined
        }
        if (
            !activeSlot?.segmentScores ||
            activeSlot.segmentScores.length === 0
        ) {
            return completedRouteCalculation.weatherSegments
        }
        return completedRouteCalculation.weatherSegments.map((seg, idx) => {
            const newScore = activeSlot.segmentScores[idx] ?? seg.score
            const newReason =
                activeSlot.segmentReasons?.[idx] ?? seg.reasonLabel
            return {
                ...seg,
                score: newScore,
                color: getScoreColor(newScore),
                reasonLabel: newReason,
            }
        })
    }, [completedRouteCalculation, activeSlot])

    const coloredMapSegments = useMemo<RouteMapSegment[]>(() => {
        if (!activeWeatherSegments) {
            return []
        }

        return [...activeWeatherSegments]
            .sort((first, second) => first.segmentIndex - second.segmentIndex)
            .flatMap((segment) => {
                const positions = segment.geometry
                    ? toLatLngPositions(segment.geometry.coordinates)
                    : []

                return positions.length >= 2
                    ? [{ positions, color: segment.color }]
                    : []
            })
    }, [activeWeatherSegments])

    useEffect(() => {
        if (selectedProfile) {
            window.localStorage.setItem(
                SELECTED_PROFILE_STORAGE_KEY,
                selectedProfile,
            )
            return
        }

        window.localStorage.removeItem(SELECTED_PROFILE_STORAGE_KEY)
    }, [selectedProfile])

    useEffect(() => {
        const trimmedInput = startInput.trim()
        const requestId = ++startRequestId.current
        setLastCompletedStartQuery((prev) => (prev === null ? prev : null))

        if (trimmedInput.length < MINIMUM_SEARCH_LENGTH || selectedStart) {
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
                if (!isLatestRequest(requestId, startRequestId.current)) {
                    return
                }

                setStartResults((result.data?.searchPlaces ?? []) as Place[])
                setLastCompletedStartQuery(trimmedInput)
            })
        }, SEARCH_DEBOUNCE_MS)

        return () => window.clearTimeout(timeoutId)
    }, [startInput, selectedStart, searchStartPlaces])

    useEffect(() => {
        const trimmedInput = destinationInput.trim()
        const requestId = ++destinationRequestId.current
        setLastCompletedDestinationQuery((prev) =>
            prev === null ? prev : null,
        )

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
                    !isLatestRequest(requestId, destinationRequestId.current)
                ) {
                    return
                }

                setDestinationResults(
                    (result.data?.searchPlaces ?? []) as Place[],
                )
                setLastCompletedDestinationQuery(trimmedInput)
            })
        }, SEARCH_DEBOUNCE_MS)

        return () => window.clearTimeout(timeoutId)
    }, [destinationInput, selectedDestination, searchDestinationPlaces])

    useEffect(() => {
        const calculation = routeCalculationData?.routeCalculation

        if (!calculation || calculation.id !== currentCalculationId) {
            return
        }

        setRouteCalculation(calculation)

        if (
            calculation.status === 'COMPLETED' ||
            calculation.status === 'FAILED'
        ) {
            stopPollingSafely()
        }
    }, [currentCalculationId, routeCalculationData])

    useEffect(() => {
        return () => stopPollingSafely()
    }, [])

    useEffect(() => {
        stopPollingSafely()
        setCurrentCalculationId((prev) => (prev === null ? prev : null))
        setRouteCalculation((prev) => (prev === null ? prev : null))
        setRouteCalculationError((prev) => (prev === null ? prev : null))
        setHoveredSegmentIndex(null)
    }, [selectedStart, selectedDestination, selectedProfile, departureTime])

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

    function handleStartSelection(place: Place): void {
        setSelectedStart(place)
        setStartInput(place.displayName)
        setStartResults([])
        setLastCompletedStartQuery(null)
    }

    function handleDestinationSelection(place: Place): void {
        setSelectedDestination(place)
        setDestinationInput(place.displayName)
        setDestinationResults([])
        setLastCompletedDestinationQuery(null)
    }

    function handleDepartureTimeChange(
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        setDepartureTime(event.target.value)
        setMinimumDepartureTime(createMinimumDepartureTime())
        setMaximumDepartureTime(createMaximumDepartureTime())
    }

    function handleGpxExport(): void {
        if (!completedRouteCalculation?.geometry || !routeCanBeExportedAsGpx) {
            return
        }

        downloadRouteAsGpx({
            startLabel: completedRouteCalculation.start.label,
            destinationLabel: completedRouteCalculation.destination.label,
            geometry: completedRouteCalculation.geometry,
        })
    }

    async function handleRouteCalculation(): Promise<void> {
        if (!canCalculateRoute || !routeCoordinates || !selectedProfile) {
            return
        }

        stopPollingSafely()
        setRouteCalculationError(null)
        setRouteCalculation(null)
        setOptimizationData(null)
        setSelectedSlotIndex(null)
        setHoveredSegmentIndex(null)

        if (!isAuthenticated) {
            setRouteCalculationError(
                'Du musst angemeldet sein, um Routen berechnen zu können. Bitte melde dich an.',
            )
            return
        }

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
            const payload = result.data?.startRouteCalculation

            if (!payload) {
                setRouteCalculationError(
                    'Die Routenberechnung konnte nicht gestartet werden.',
                )
                return
            }

            if (payload.errors.length > 0) {
                const isUnverified = payload.errors.some(
                    (error) => error.code === 'EMAIL_NOT_VERIFIED',
                )
                const isSessionInvalid = payload.errors.some(
                    (error) =>
                        error.code === 'SESSION_INVALID' ||
                        error.code === 'UNAUTHORIZED',
                )
                if (isUnverified) {
                    setRouteCalculationError(
                        'Dein Account ist noch nicht verifiziert. Bitte bestätige den Aktivierungslink in deinem E-Mail-Postfach, um Routen berechnen zu können.',
                    )
                } else if (isSessionInvalid) {
                    setRouteCalculationError(
                        'Du musst angemeldet sein, um Routen berechnen zu können. Bitte melde dich an.',
                    )
                } else {
                    setRouteCalculationError(
                        payload.errors.map((error) => error.message).join(' '),
                    )
                }
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
                variables: { id: calculationId },
            })

            if (
                payload.calculation.status !== 'COMPLETED' &&
                payload.calculation.status !== 'FAILED'
            ) {
                try {
                    isPollingRef.current = true
                    startPolling(ROUTE_CALCULATION_POLL_INTERVAL_MS)
                } catch {
                    isPollingRef.current = false
                }
            }
        } catch (err: unknown) {
            const apolloError = err as {
                graphQLErrors?: Array<{
                    extensions?: { code?: string; classification?: string }
                    message?: string
                }>
                message?: string
            }
            const isUnverified =
                apolloError?.graphQLErrors?.some(
                    (e) => e.extensions?.code === 'EMAIL_NOT_VERIFIED',
                ) || Boolean(apolloError?.message?.includes('EMAIL_NOT_VERIFIED'))

            const isSessionInvalid =
                !isAuthenticated ||
                apolloError?.graphQLErrors?.some(
                    (e) =>
                        e.extensions?.code === 'SESSION_INVALID' ||
                        e.extensions?.classification === 'UNAUTHORIZED',
                ) ||
                Boolean(apolloError?.message?.includes('SESSION_INVALID')) ||
                Boolean(apolloError?.message?.includes('Sitzung ist ungültig'))

            if (isUnverified) {
                setRouteCalculationError(
                    'Dein Account ist noch nicht verifiziert. Bitte bestätige den Aktivierungslink in deinem E-Mail-Postfach, um Routen berechnen zu können.',
                )
            } else if (isSessionInvalid) {
                setRouteCalculationError(
                    'Du musst angemeldet sein, um Routen berechnen zu können. Bitte melde dich an.',
                )
            } else {
                setRouteCalculationError(
                    'Die Routenberechnung konnte nicht gestartet werden.',
                )
            }
        }
    }

    function statusClass(...variants: Array<string | undefined>): string {
        return [styles.statusBanner, ...variants]
            .filter((className): className is string => Boolean(className))
            .join(' ')
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
                            <p className={styles.hint} role="status">
                                Startpunkt wird gesucht …
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
                                    id="start-search-results"
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
                                                className={
                                                    styles.searchResultButton
                                                }
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
                                <span aria-hidden="true">✓</span>{' '}
                                <strong>Ausgewählt:</strong>{' '}
                                {selectedStart.displayName}
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
                            onChange={handleDestinationInputChange}
                            placeholder="Ort oder Adresse eingeben"
                            autoComplete="street-address"
                            maxLength={MAXIMUM_SEARCH_LENGTH}
                            aria-autocomplete="list"
                            aria-controls="destination-search-results"
                            aria-expanded={
                                !destinationLoading &&
                                !destinationError &&
                                !selectedDestination &&
                                destinationResults.length > 0
                            }
                        />
                        {destinationLoading && (
                            <p className={styles.hint} role="status">
                                Zielpunkt wird gesucht …
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
                            lastCompletedDestinationQuery ===
                            destinationInput.trim() &&
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
                                    id="destination-search-results"
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
                                                className={
                                                    styles.searchResultButton
                                                }
                                                onClick={() =>
                                                    handleDestinationSelection(
                                                        place,
                                                    )
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
                                <span aria-hidden="true">✓</span>{' '}
                                <strong>Ausgewählt:</strong>{' '}
                                {selectedDestination.displayName}
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
                            onChange={handleDepartureTimeChange}
                            required
                            aria-invalid={!departureTimeIsValid}
                            aria-describedby="departure-time-hint"
                        />
                        <p id="departure-time-hint" className={styles.hint}>
                            Wähle eine zukünftige Abfahrtszeit im
                            Vorhersagebereich. Möglich sind bis zu sieben Tage
                            im Voraus in 15-Minuten-Schritten.
                        </p>
                        {!departureTimeInFuture && (
                            <p className={styles.fieldError} role="alert">
                                Die Abfahrtszeit muss in der Zukunft liegen.
                            </p>
                        )}
                        {departureTimeInFuture && !departureTimeWithinForecast && (
                            <p className={styles.fieldError} role="alert">
                                Die Abfahrtszeit darf maximal sieben Tage in der
                                Zukunft liegen.
                            </p>
                        )}
                    </div>

                    <button
                        type="button"
                        className={styles.calculateButton}
                        disabled={!canCalculateRoute || routeCalculationIsRunning}
                        aria-busy={routeCalculationIsRunning}
                        onClick={() => void handleRouteCalculation()}
                    >
                        {routeCalculationIsRunning
                            ? 'Route wird berechnet …'
                            : 'Route berechnen'}
                    </button>
                </div>

                {startRouteCalculationLoading && (
                    <div
                        className={statusClass(styles.statusBannerInfo)}
                        role="status"
                        aria-live="polite"
                    >
                        <span>Die Routenberechnung wird gestartet.</span>
                    </div>
                )}
                {!startRouteCalculationLoading &&
                    routeCalculation?.status === 'PENDING' && (
                        <div
                            className={statusClass(styles.statusBannerInfo)}
                            role="status"
                            aria-live="polite"
                        >
                            <span>
                                Die Routenberechnung wartet auf die
                                Verarbeitung.
                            </span>
                        </div>
                    )}
                {!startRouteCalculationLoading &&
                    routeCalculation?.status === 'PROCESSING' && (
                        <div
                            className={statusClass(styles.statusBannerInfo)}
                            role="status"
                            aria-live="polite"
                        >
                            <span>Die Route wird aktuell verarbeitet.</span>
                        </div>
                    )}
                {routeCalculation?.status === 'COMPLETED' && (
                    <div
                        className={statusClass(styles.statusBannerSuccess)}
                        role="status"
                    >
                        <span>
                            Die Routenberechnung wurde erfolgreich
                            abgeschlossen.
                        </span>
                    </div>
                )}
                {routeCalculation?.status === 'FAILED' && (
                    <div
                        className={statusClass(styles.statusBannerError)}
                        role="alert"
                    >
                        <span>
                            {routeCalculation.error?.message ??
                                'Für diese Anfrage konnte keine Route berechnet werden.'}
                        </span>
                    </div>
                )}
                {routeCalculationError && (
                    <div
                        className={statusClass(styles.statusBannerError)}
                        role="alert"
                    >
                        <span>
                            {routeCalculationError}
                            {(!isAuthenticated ||
                                routeCalculationError.includes('angemeldet sein')) && (
                                <span style={{ display: 'block', marginTop: '0.4rem' }}>
                                    <Link
                                        to="/login"
                                        style={{
                                            textDecoration: 'underline',
                                            fontWeight: 700,
                                            color: 'inherit',
                                            marginRight: '0.75rem',
                                        }}
                                    >
                                        Jetzt anmelden
                                    </Link>
                                    <Link
                                        to="/register"
                                        style={{
                                            textDecoration: 'underline',
                                            fontWeight: 700,
                                            color: 'inherit',
                                        }}
                                    >
                                        Konto erstellen
                                    </Link>
                                </span>
                            )}
                        </span>
                    </div>
                )}
                {!routeCalculationError && startRouteCalculationGraphQlError && (
                    <div
                        className={statusClass(styles.statusBannerError)}
                        role="alert"
                    >
                        <span>
                            {'graphQLErrors' in startRouteCalculationGraphQlError &&
                            Array.isArray((startRouteCalculationGraphQlError as { graphQLErrors?: Array<{ extensions?: { code?: string; classification?: string } }> }).graphQLErrors) &&
                            (startRouteCalculationGraphQlError as { graphQLErrors: Array<{ extensions?: { code?: string; classification?: string } }> }).graphQLErrors.some(
                                (e) => e.extensions?.code === 'EMAIL_NOT_VERIFIED',
                            )
                                ? 'Dein Account ist noch nicht verifiziert. Bitte bestätige den Aktivierungslink in deinem E-Mail-Postfach, um Routen berechnen zu können.'
                                : ('graphQLErrors' in startRouteCalculationGraphQlError &&
                                   Array.isArray((startRouteCalculationGraphQlError as { graphQLErrors?: Array<{ extensions?: { code?: string; classification?: string } }> }).graphQLErrors) &&
                                   (startRouteCalculationGraphQlError as { graphQLErrors: Array<{ extensions?: { code?: string; classification?: string } }> }).graphQLErrors.some(
                                       (e) =>
                                           e.extensions?.code === 'SESSION_INVALID' ||
                                           e.extensions?.classification === 'UNAUTHORIZED',
                                   )) || !isAuthenticated
                                ? (
                                    <>
                                        Du musst angemeldet sein, um Routen berechnen zu können.{' '}
                                        <Link
                                            to="/login"
                                            style={{
                                                textDecoration: 'underline',
                                                fontWeight: 700,
                                                color: 'inherit',
                                            }}
                                        >
                                            Jetzt anmelden
                                        </Link>
                                    </>
                                )
                                : 'Die Routenberechnung konnte wegen eines Kommunikationsfehlers nicht gestartet werden.'}
                        </span>
                    </div>
                )}
                {routeCalculationQueryError && (
                    <div
                        className={statusClass(styles.statusBannerError)}
                        role="alert"
                    >
                        <span>
                            Der aktuelle Stand der Routenberechnung konnte nicht
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
                            <dt className={styles.statLabel}>Distanz</dt>
                            <dd className={styles.statValue}>
                                {completedRouteCalculation.distanceMeters !==
                                null
                                    ? formatDistance(
                                        completedRouteCalculation.distanceMeters,
                                    )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>
                        <div className={styles.statCard}>
                            <dt className={styles.statLabel}>Fahrtdauer</dt>
                            <dd className={styles.statValue}>
                                {completedRouteCalculation.durationSeconds !==
                                null
                                    ? formatDuration(
                                        completedRouteCalculation.durationSeconds,
                                    )
                                    : 'Nicht verfügbar'}
                            </dd>
                        </div>
                        <div className={styles.statCard}>
                            <dt className={styles.statLabel}>Ankunftszeit</dt>
                            <dd className={styles.statValue}>
                                {activeSlot
                                    ? `${activeSlot.arrivalTime} Uhr (Start ${activeSlot.departureTime} Uhr)`
                                    : completedRouteCalculation.arrivalTime
                                        ? formatArrivalTime(
                                            completedRouteCalculation.arrivalTime,
                                        )
                                        : 'Nicht verfügbar'}
                            </dd>
                        </div>

                        {(activeSlot !== null ||
                            typeof completedRouteCalculation.weatherScore ===
                            'number') && (
                            <div className={styles.statCard}>
                                <dt className={styles.statLabel}>
                                    Wetterscore
                                </dt>
                                <dd className={styles.statValue}>
                                    {activeSlot?.routeScore ??
                                        completedRouteCalculation.weatherScore}{' '}
                                    / 100
                                </dd>
                            </div>
                        )}
                    </dl>

                    {(activeSlot
                        ? activeSlot.hasThunderCap
                        : completedRouteCalculation.thunderstormCapApplied) && (
                        <div
                            className={statusClass(styles.statusBannerError)}
                            role="status"
                            style={{ marginTop: '0.75rem' }}
                        >
                            <span>
                                Erhöhtes Gewitterrisiko auf der Route erkannt.
                                Der Gesamtwetterscore wurde auf maximal 50
                                begrenzt.
                            </span>
                        </div>
                    )}
                </section>
            )}

            {optimizationData && optimizationData.departureSlots.length > 0 && (
                <DepartureTimeSlider
                    slots={optimizationData.departureSlots}
                    recommendedSlotIndex={optimizationData.recommendedSlotIndex}
                    selectedSlotIndex={
                        selectedSlotIndex ??
                        optimizationData.recommendedSlotIndex
                    }
                    optimalWindowSummary={optimizationData.optimalWindowSummary}
                    initialDepartureIndex={initialDepartureSlotIndex}
                    onSelectSlot={(idx) => {
                        setSelectedSlotIndex(idx)
                        const chosenSlot = optimizationData.departureSlots[idx]
                        if (chosenSlot?.departureIso) {
                            setUserSelectedDepartureIso(chosenSlot.departureIso)
                        }
                    }}
                />
            )}

            <section
                className={styles.mapWrapper}
                aria-label="Kartendarstellung der Route"
            >
                <RouteMap
                    startPlace={selectedStart}
                    destinationPlace={selectedDestination}
                    routeSegments={coloredMapSegments}
                    geometry={completedRouteCalculation?.geometry ?? null}
                    weatherSegments={activeWeatherSegments}
                    hoveredSegmentIndex={hoveredSegmentIndex}
                    onHoverSegment={setHoveredSegmentIndex}
                />

                {activeWeatherSegments && activeWeatherSegments.length > 0 && (
                    <MeteogramDock
                        segments={activeWeatherSegments}
                        totalDistanceMeters={
                            completedRouteCalculation?.distanceMeters
                        }
                        hoveredSegmentIndex={hoveredSegmentIndex}
                        onHoverSegment={setHoveredSegmentIndex}
                        onSelectSegment={setHoveredSegmentIndex}
                        activityProfileCode={selectedProfile}
                    />
                )}
            </section>

            {completedRouteCalculation && (
                <AiWeatherAdvisorPanel
                    analysis={aiAnalysis}
                    loading={aiAnalysisLoading}
                    error={aiAnalysisError}
                    errorCode={aiAnalysisErrorCode}
                    isAuthenticated={isAuthenticated}
                    onRefresh={handleRefreshAiAnalysis}
                    onHoverSegment={setHoveredSegmentIndex}
                />
            )}

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
                                    color: 'var(--color-primary, #2563eb)',
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
        </main>
    )
}