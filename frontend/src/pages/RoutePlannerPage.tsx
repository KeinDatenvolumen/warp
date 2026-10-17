import { useEffect, useState } from 'react'
import { useLazyQuery } from '@apollo/client/react'

import { SEARCH_PLACES_QUERY } from '../api/searchPlacesQuery'
import RouteMap from '../components/RouteMap/RouteMap'
import ActivityProfileSelector from '../features/activity-profiles/ActivityProfileSelector'
import type { ActivityProfileCode } from '../types/activityProfile'

import styles from './PagePlaceholder.module.css'

const SELECTED_PROFILE_STORAGE_KEY = 'warp-selected-activity-profile'

const MINIMUM_SEARCH_LENGTH = 3
const SEARCH_RESULT_LIMIT = 5
const SEARCH_DEBOUNCE_MS = 400

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

    const [
        searchStartPlaces,
        {
            data: startData,
            loading: startLoading,
            error: startError,
        },
    ] = useLazyQuery(SEARCH_PLACES_QUERY)

    const [
        searchDestinationPlaces,
        {
            data: destinationData,
            loading: destinationLoading,
            error: destinationError,
        },
    ] = useLazyQuery(SEARCH_PLACES_QUERY)

    useEffect(() => {
        const trimmedInput = startInput.trim()

        if (trimmedInput.length < MINIMUM_SEARCH_LENGTH) {
            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchStartPlaces({
                variables: {
                    query: trimmedInput,
                    limit: SEARCH_RESULT_LIMIT,
                },
            })
        }, SEARCH_DEBOUNCE_MS)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [startInput, searchStartPlaces])

    useEffect(() => {
        const trimmedInput = destinationInput.trim()

        if (trimmedInput.length < MINIMUM_SEARCH_LENGTH) {
            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchDestinationPlaces({
                variables: {
                    query: trimmedInput,
                    limit: SEARCH_RESULT_LIMIT,
                },
            })
        }, SEARCH_DEBOUNCE_MS)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [destinationInput, searchDestinationPlaces])

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
                    <label htmlFor="start-input">Startpunkt</label>

                    <input
                        id="start-input"
                        name="start"
                        type="text"
                        value={startInput}
                        onChange={(event) =>
                            setStartInput(event.target.value)
                        }
                        placeholder="Ort oder Adresse eingeben"
                        autoComplete="street-address"
                        maxLength={200}
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

                    {!startLoading && !startError && startData && (
                        <p aria-live="polite">
                            {startData.searchPlaces.length}{' '}
                            {startData.searchPlaces.length === 1
                                ? 'Ergebnis gefunden'
                                : 'Ergebnisse gefunden'}
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
                        onChange={(event) =>
                            setDestinationInput(event.target.value)
                        }
                        placeholder="Ort oder Adresse eingeben"
                        autoComplete="street-address"
                        maxLength={200}
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
                        destinationData && (
                            <p aria-live="polite">
                                {
                                    destinationData.searchPlaces
                                        .length
                                }{' '}
                                {destinationData.searchPlaces
                                    .length === 1
                                    ? 'Ergebnis gefunden'
                                    : 'Ergebnisse gefunden'}
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