import { useEffect, useState } from "react"
import { useLazyQuery } from '@apollo/client/react'
import RouteMap from '../components/RouteMap/RouteMap'
import { SEARCH_PLACES_QUERY } from '../api/searchPlacesQuery'
import styles from './PagePlaceholder.module.css'

export default function RoutePlannerPage() {
    const [startInput, setStartInput] = useState("");
    const [destinationInput, setDestinationInput] = useState("");

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

        if (trimmedInput.length < 3) {
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
    }, [startInput, searchStartPlaces])

    useEffect(() => {
        const trimmedInput = destinationInput.trim()

        if (trimmedInput.length < 3) {
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
    }, [destinationInput, searchDestinationPlaces])

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
                                    onChange={(event) => setStartInput(event.target.value)}
                                    placeholder="Ort oder Adresse eingeben"
                                    autoComplete="street-address"
                                />
                                {startLoading && <p>Startpunkt wird gesucht...</p>}

                                {startError && (
                                <p role="alert">
                                Der Startpunkt konnte nicht gesucht werden.
                                </p>
                                )}

                                {startData && (
                                <p>
                                {startData.searchPlaces.length} Ergebnisse gefunden
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
                                    onChange={(event) =>
                                        setDestinationInput(event.target.value)
                                    }
                                    placeholder="Ort oder Adresse eingeben"
                                    autoComplete="street-address"
                                />
                                {destinationLoading && <p>Zielpunkt wird gesucht...</p>}

                                {destinationError && (
                                    <p role="alert">
                                        Der Zielpunkt konnte nicht gesucht werden.
                                    </p>
                                )}

                                {destinationData && (
                                    <p>
                                        {destinationData.searchPlaces.length} Ergebnisse gefunden
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