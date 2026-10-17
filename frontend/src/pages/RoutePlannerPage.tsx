import { useEffect, useRef, useState } from 'react'
import { useLazyQuery } from '@apollo/client/react'

import { SEARCH_PLACES_QUERY } from '../api/searchPlacesQuery'
import RouteMap from '../components/RouteMap/RouteMap'
import ActivityProfileSelector from '../features/activity-profiles/ActivityProfileSelector'
import type { ActivityProfileCode } from '../types/activityProfile'
import type { Place } from '../types/searchPlaces'

import styles from './PagePlaceholder.module.css'

const SELECTED_PROFILE_STORAGE_KEY = 'warp-selected-activity-profile'

const MINIMUM_SEARCH_LENGTH = 3
const SEARCH_RESULT_LIMIT = 5
const SEARCH_DEBOUNCE_MS = 400
const MAXIMUM_SEARCH_LENGTH = 200

function readSelectedProfile(): ActivityProfileCode | null {
    const value = localStorage.getItem(SELECTED_PROFILE_STORAGE_KEY)

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
    const [selectedProfile, setSelectedProfile] =
        useState<ActivityProfileCode | null>(readSelectedProfile)

    const [startInput, setStartInput] = useState('')
    const [destinationInput, setDestinationInput] = useState('')

    const [selectedStart, setSelectedStart] =
        useState<Place | null>(null)

    const [selectedDestination, setSelectedDestination] =
        useState<Place | null>(null)

    const [startResults, setStartResults] = useState<Place[]>([])
    const [destinationResults, setDestinationResults] =
        useState<Place[]>([])

    const [lastCompletedStartQuery, setLastCompletedStartQuery] =
        useState<string | null>(null)

    const [
        lastCompletedDestinationQuery,
        setLastCompletedDestinationQuery,
    ] = useState<string | null>(null)

    const startRequestId = useRef(0)
    const destinationRequestId = useRef(0)

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

    useEffect(() => {
        if (selectedProfile) {
            localStorage.setItem(
                SELECTED_PROFILE_STORAGE_KEY,
                selectedProfile,
            )
        } else {
            localStorage.removeItem(SELECTED_PROFILE_STORAGE_KEY)
        }
    }, [selectedProfile])

    useEffect(() => {
        const trimmedInput = startInput.trim()
        const requestId = ++startRequestId.current

        setLastCompletedStartQuery(null)

        if (
            trimmedInput.length < MINIMUM_SEARCH_LENGTH ||
            selectedStart
        ) {
            setStartResults([])
            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchStartPlaces({
                variables: {
                    query: trimmedInput,
                    limit: SEARCH_RESULT_LIMIT,
                },
            }).then((result) => {
                if (requestId !== startRequestId.current) {
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

        setLastCompletedDestinationQuery(null)

        if (
            trimmedInput.length < MINIMUM_SEARCH_LENGTH ||
            selectedDestination
        ) {
            setDestinationResults([])
            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchDestinationPlaces({
                variables: {
                    query: trimmedInput,
                    limit: SEARCH_RESULT_LIMIT,
                },
            }).then((result) => {
                if (requestId !== destinationRequestId.current) {
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

    function handleStartInputChange(
        event: React.ChangeEvent<HTMLInputElement>,
    ) {
        setStartInput(event.target.value)
        setSelectedStart(null)
        setStartResults([])
        setLastCompletedStartQuery(null)
    }

    function handleDestinationInputChange(
        event: React.ChangeEvent<HTMLInputElement>,
    ) {
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

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Routenplanung</p>

                <h1>Routenplaner</h1>

                <p className={styles.description}>
                    Wähle ein Aktivitätsprofil sowie Start und Ziel für
                    deine zukünftige wetterabhängige Routenplanung.
                </p>
            </section>

            <div style={{ marginTop: '1.5rem' }}>
                <ActivityProfileSelector
                    selectedProfile={selectedProfile}
                    onSelect={setSelectedProfile}
                />
            </div>

            <section
                className={styles.card}
                style={{ marginTop: '1.5rem' }}
                aria-labelledby="route-input-heading"
            >
                <h2 id="route-input-heading">Start und Ziel</h2>

                <div>
                    <label htmlFor="start-input">
                        Startpunkt
                    </label>

                    <input
                        id="start-input"
                        name="start"
                        type="text"
                        value={startInput}
                        onChange={handleStartInputChange}
                        placeholder="Ort oder Adresse eingeben"
                        autoComplete="street-address"
                        maxLength={MAXIMUM_SEARCH_LENGTH}
                    />

                    {startLoading && (
                        <p aria-live="polite">
                            Startpunkt wird gesucht...
                        </p>
                    )}

                    {startError && (
                        <p role="alert">
                            Der Startpunkt konnte nicht gesucht werden.
                        </p>
                    )}

                    {!startLoading &&
                        !startError &&
                        !selectedStart &&
                        lastCompletedStartQuery ===
                        startInput.trim() &&
                        startResults.length === 0 && (
                            <p role="status">
                                Kein passender Startpunkt gefunden.
                            </p>
                        )}

                    {!startLoading &&
                        !startError &&
                        !selectedStart &&
                        startResults.length > 0 && (
                            <>
                                <p aria-live="polite">
                                    {startResults.length}{' '}
                                    {startResults.length === 1
                                        ? 'Ergebnis gefunden'
                                        : 'Ergebnisse gefunden'}
                                </p>

                                <ul aria-label="Suchergebnisse für den Startpunkt">
                                    {startResults.map((place) => (
                                        <li key={place.placeId}>
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    handleStartSelection(
                                                        place,
                                                    )
                                                }
                                            >
                                                {place.displayName}
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            </>
                        )}

                    {selectedStart && (
                        <p role="status">
                            Ausgewählter Startpunkt:{' '}
                            {selectedStart.displayName}
                            {' – '}
                            {selectedStart.latitude},{' '}
                            {selectedStart.longitude}
                        </p>
                    )}
                </div>

                <div>
                    <label htmlFor="destination-input">
                        Zielpunkt
                    </label>

                    <input
                        id="destination-input"
                        name="destination"
                        type="text"
                        value={destinationInput}
                        onChange={handleDestinationInputChange}
                        placeholder="Ort oder Adresse eingeben"
                        autoComplete="street-address"
                        maxLength={MAXIMUM_SEARCH_LENGTH}
                    />

                    {destinationLoading && (
                        <p aria-live="polite">
                            Zielpunkt wird gesucht...
                        </p>
                    )}

                    {destinationError && (
                        <p role="alert">
                            Der Zielpunkt konnte nicht gesucht werden.
                        </p>
                    )}

                    {!destinationLoading &&
                        !destinationError &&
                        !selectedDestination &&
                        lastCompletedDestinationQuery ===
                        destinationInput.trim() &&
                        destinationResults.length === 0 && (
                            <p role="status">
                                Kein passender Zielpunkt gefunden.
                            </p>
                        )}

                    {!destinationLoading &&
                        !destinationError &&
                        !selectedDestination &&
                        destinationResults.length > 0 && (
                            <>
                                <p aria-live="polite">
                                    {destinationResults.length}{' '}
                                    {destinationResults.length === 1
                                        ? 'Ergebnis gefunden'
                                        : 'Ergebnisse gefunden'}
                                </p>

                                <ul aria-label="Suchergebnisse für den Zielpunkt">
                                    {destinationResults.map((place) => (
                                        <li key={place.placeId}>
                                            <button
                                                type="button"
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
                            </>
                        )}

                    {selectedDestination && (
                        <p role="status">
                            Ausgewählter Zielpunkt:{' '}
                            {selectedDestination.displayName}
                            {' – '}
                            {selectedDestination.latitude},{' '}
                            {selectedDestination.longitude}
                        </p>
                    )}
                </div>
            </section>

            <div style={{ marginTop: '1.5rem' }}>
                <RouteMap />
            </div>
        </main>
    )
}