import { useEffect, useRef, useState } from "react"
import { useLazyQuery } from '@apollo/client/react'
import RouteMap from '../components/RouteMap/RouteMap'
import { SEARCH_PLACES_QUERY } from '../api/searchPlacesQuery'
import styles from './PagePlaceholder.module.css'
import type { Place } from '../types/searchPlaces'
import { isLatestRequest } from '../utils/latestRequest'

export default function RoutePlannerPage() {
    const [startInput, setStartInput] = useState("");
    const [destinationInput, setDestinationInput] = useState("");

    const [startResults, setStartResults] =
            useState < Place[] >([])

    const [destinationResults, setDestinationResults] =
            useState < Place[] >([])

    const startRequestId = useRef(0)
    const destinationRequestId = useRef(0)

    const [selectedStart, setSelectedStart] =
            useState<Place | null>(null)

    const [selectedDestination, setSelectedDestination] =
            useState<Place | null>(null)

    const routeCoordinates =
        selectedStart && selectedDestination
            ? {
                  start: {
                      latitude: selectedStart.latitude,
                      longitude: selectedStart.longitude,
                  },
                  destination: {
                      latitude: selectedDestination.latitude,
                      longitude: selectedDestination.longitude,
                  },
              }
            : null

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
        const trimmedInput = startInput.trim()
        const requestId = ++startRequestId.current

        if (trimmedInput.length < 3 || selectedStart) {
            setStartResults([])
            return
        }

        const timeoutId = window.setTimeout(() => {
            void searchStartPlaces({
                variables: {
                    query: trimmedInput,
                    limit: 5,
                },
            }).then((result) => {
                if (!isLatestRequest(requestId, startRequestId.current)) {
                    return
                }

                setStartResults(result.data?.searchPlaces ?? [])
            })
        }, 400)

        return () => {
            window.clearTimeout(timeoutId)
        }
    }, [startInput, selectedStart, searchStartPlaces])


   useEffect(() => {
       const trimmedInput = destinationInput.trim()
       const requestId = ++destinationRequestId.current

       if (trimmedInput.length < 3 || selectedDestination) {
           setDestinationResults([])
           return
       }

       const timeoutId = window.setTimeout(() => {
           void searchDestinationPlaces({
               variables: {
                   query: trimmedInput,
                   limit: 5,
               },
           }).then((result) => {
               if (!isLatestRequest(requestId, destinationRequestId.current)) {
                   return
               }

               setDestinationResults(
                   result.data?.searchPlaces ?? []
               )
           })
       }, 400)

       return () => {
           window.clearTimeout(timeoutId)
       }
   }, [
       destinationInput,
       selectedDestination,
       searchDestinationPlaces,
   ])

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
                                    maxLength={200}
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

                                {!startLoading &&
                                    !startError &&
                                    startInput.trim().length >= 3 &&
                                    !selectedStart &&
                                    startResults.length === 0 && (
                                        <p role="status">
                                            Kein passender Startpunkt gefunden.
                                        </p>
                                    )}

                                {!selectedStart &&
                                    startResults.length > 0 && (
                                        <ul aria-label="Suchergebnisse für den Startpunkt">
                                            {startResults.map((place) => (
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
                                    maxLength={200}
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

                                {!destinationLoading &&
                                    !destinationError &&
                                    destinationInput.trim().length >= 3 &&
                                    !selectedDestination &&
                                    destinationResults.length === 0 && (
                                        <p role="status">
                                            Kein passender Zielpunkt gefunden.
                                        </p>
                                    )}

                                {!selectedDestination &&
                                    destinationResults.length > 0 && (
                                        <ul aria-label="Suchergebnisse für den Zielpunkt">
                                            {destinationResults.map((place) => (
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

                            <button
                                    type="button"
                                    disabled={!routeCoordinates}
                                >
                                    Route berechnen
                            </button>

                        </section>

            <div style={{marginTop: '1.5rem'}}>
                <RouteMap
                    startPlace={selectedStart}
                    destinationPlace={selectedDestination}
                />
            </div>
        </main>
    )
}