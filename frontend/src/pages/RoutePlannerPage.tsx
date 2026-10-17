import { useEffect, useState } from "react"
import { useLazyQuery } from '@apollo/client/react'
import RouteMap from '../components/RouteMap/RouteMap'
import { SEARCH_PLACES_QUERY } from '../api/searchPlacesQuery'
import styles from './PagePlaceholder.module.css'
import type { Place } from '../types/searchPlaces'

export default function RoutePlannerPage() {
    const [startInput, setStartInput] = useState("");
    const [destinationInput, setDestinationInput] = useState("");

    const [selectedStart, setSelectedStart] =
            useState<Place | null>(null)

    const [selectedDestination, setSelectedDestination] =
            useState<Place | null>(null)

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

        if (trimmedInput.length < 3 || selectedStart) {
            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchStartPlaces({
                variables: {
                    query: trimmedInput,
                    limit: 5,
                },
            })
        }, 400)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [startInput, selectedStart, searchStartPlaces])

    useEffect(() => {
        const trimmedInput = destinationInput.trim()

        if (trimmedInput.length < 3 || selectedDestination) {
            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchDestinationPlaces({
                variables: {
                    query: trimmedInput,
                    limit: 5,
                },
            })
        }, 400)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [destinationInput, selectedDestination, searchDestinationPlaces])

    function handleStartSelection(place: Place) {
        setSelectedStart(place)
        setStartInput(place.displayName)
    }

    function handleDestinationSelection(place: Place) {
        setSelectedDestination(place)
        setDestinationInput(place.displayName)
    }

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Routenplanung</p>
                <h1>Routenplaner</h1>
                <p className={styles.description}>
                    Hier entsteht später die wetterabhängige Routenplanung.
                </p>
            </section>

             <section aria-labelledby="route-input-heading">
                            <h2 id="route-input-heading">Start und Ziel</h2>

                            <div>
                                <label htmlFor="start-input">Startpunkt</label>

                                <input
                                    id="start-input"
                                    name="start"
                                    type="text"
                                    value={startInput}
                                    onChange={(event) => {
                                            setStartInput(event.target.value)
                                            setSelectedStart(null)
                                        }}
                                    placeholder="Ort oder Adresse eingeben"
                                    autoComplete="street-address"
                                />
                                {startLoading && <p>Startpunkt wird gesucht...</p>}

                                {startError && (
                                <p role="alert">
                                Der Startpunkt konnte nicht gesucht werden.
                                </p>
                                )}

                                {startData &&
                                    !startLoading &&
                                    !startError &&
                                    startInput.trim().length >= 3 &&
                                    !selectedStart &&
                                    startData.searchPlaces.length === 0 && (
                                        <p role="status">
                                            Kein passender Startpunkt gefunden.
                                        </p>
                                    )}

                                {startData &&
                                    !selectedStart &&
                                    startData.searchPlaces.length > 0 && (
                                        <ul aria-label="Suchergebnisse für den Startpunkt">
                                            {startData.searchPlaces.map((place) => (
                                                <li key={place.placeId}>
                                                    <button
                                                        type="button"
                                                        onClick={() => handleStartSelection(place)}
                                                    >
                                                        {place.displayName}
                                                    </button>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                {selectedStart && (
                                    <p role="status">
                                        Ausgewählter Startpunkt: {selectedStart.displayName}
                                        {' – '}
                                        {selectedStart.latitude}, {selectedStart.longitude}
                                    </p>
                                )}
                            </div>

                            <div>
                                <label htmlFor="destination-input">Zielpunkt</label>

                                <input
                                    id="destination-input"
                                    name="destination"
                                    type="text"
                                    value={destinationInput}
                                    onChange={(event) => {
                                            setDestinationInput(event.target.value)
                                            setSelectedDestination(null)
                                        }}
                                    placeholder="Ort oder Adresse eingeben"
                                    autoComplete="street-address"
                                />
                                {destinationLoading && <p>Zielpunkt wird gesucht...</p>}

                                {destinationError && (
                                    <p role="alert">
                                        Der Zielpunkt konnte nicht gesucht werden.
                                    </p>
                                )}

                                {destinationData &&
                                    !destinationLoading &&
                                    !destinationError &&
                                    destinationInput.trim().length >= 3 &&
                                    !selectedDestination &&
                                    destinationData.searchPlaces.length === 0 && (
                                        <p role="status">
                                            Kein passender Zielpunkt gefunden.
                                        </p>
                                    )}

                                {destinationData &&
                                    !selectedDestination &&
                                    destinationData.searchPlaces.length > 0 && (
                                        <ul aria-label="Suchergebnisse für den Zielpunkt">
                                            {destinationData.searchPlaces.map((place) => (
                                                <li key={place.placeId}>
                                                    <button
                                                        type="button"
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
                                    <p role="status">
                                        Ausgewählter Zielpunkt: {selectedDestination.displayName}
                                        {' – '}
                                        {selectedDestination.latitude},{' '}
                                        {selectedDestination.longitude}
                                    </p>
                                )}
                            </div>
                        </section>

            <div style={{marginTop: '1.5rem'}}>
                <RouteMap />
            </div>
        </main>
    )
}