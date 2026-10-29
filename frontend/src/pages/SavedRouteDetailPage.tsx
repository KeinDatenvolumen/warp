import {useMemo, useState} from 'react'
import {useMutation, useQuery} from '@apollo/client/react'
import {
    Link,
    useNavigate,
    useParams,
} from 'react-router'

import {extractGraphQlErrorCodes} from '../api/graphqlErrors'
import {
    DELETE_SAVED_ROUTE_MUTATION,
    SAVED_ROUTE_QUERY,
} from '../api/savedRoutesApi'
import RouteMap from '../components/RouteMap/RouteMap'
import ConfirmDialog from '../components/ui/ConfirmDialog/ConfirmDialog'
import Loader from '../components/ui/Loader/Loader'
import MessageCard from '../components/ui/MessageCard/MessageCard'
import {
    formatDateTime,
    formatDistance,
    formatDuration,
} from '../features/saved-routes/format'
import {
    buildRouteMapSegments,
} from '../features/saved-routes/routeGeometry'
import listStyles from '../features/saved-routes/SavedRoutes.module.css'
import type {RoutePlace} from '../types/savedRoute'
import type {Place} from '../types/searchPlaces'
import styles from './PagePlaceholder.module.css'
import FavoriteButton from '../features/saved-routes/FavoriteButton'

const DELETE_FAILED_MESSAGE =
    'Die Route konnte nicht gelöscht werden. Bitte versuche es erneut.'

function toPlace(
    id: string,
    place: RoutePlace,
): Place {
    return {
        placeId: id,
        displayName: place.label,
        latitude: place.latitude,
        longitude: place.longitude,
    }
}

export default function SavedRouteDetailPage() {
    const {routeId} = useParams()
    const navigate = useNavigate()
    const [confirmOpen, setConfirmOpen] = useState(false)
    const [deleteError, setDeleteError] = useState<string>()
    const [favoriteError, setFavoriteError] = useState<string>()

    const [confirmOpen, setConfirmOpen] =
        useState(false)

    const [deleteError, setDeleteError] =
        useState<string>()

    const {
        data,
        loading,
        error,
        refetch,
    } = useQuery(SAVED_ROUTE_QUERY, {
        variables: {
            id: routeId ?? '',
        },
        skip: !routeId,
        fetchPolicy: 'cache-and-network',
    })

    const [
        deleteSavedRoute,
        {loading: deleting},
    ] = useMutation(DELETE_SAVED_ROUTE_MUTATION)

    const route = data?.savedRoute ?? null

    const mapSegments = useMemo(
        () =>
            route
                ? buildRouteMapSegments(route)
                : [],
        [route],
    )

    const startPlace = useMemo(
        () =>
            route
                ? toPlace(
                    'saved-start',
                    route.start,
                )
                : null,
        [route],
    )

    const destinationPlace = useMemo(
        () =>
            route
                ? toPlace(
                    'saved-destination',
                    route.destination,
                )
                : null,
        [route],
    )

    async function handleConfirmDelete(): Promise<void> {
        if (!routeId || deleting) {
            return
        }

        setConfirmOpen(false)
        setDeleteError(undefined)

        try {
            const result = await deleteSavedRoute({
                variables: {
                    id: routeId,
                },
                update: (cache) => {
                    cache.evict({
                        fieldName: 'savedRoutes',
                    })
                    cache.gc()
                },
            })

            const errors =
                result.data?.deleteSavedRoute.errors ?? []

            if (errors.length > 0) {
                setDeleteError(
                    errors[0]?.message ??
                    DELETE_FAILED_MESSAGE,
                )

                return
            }

            navigate('/saved-routes', {
                replace: true,
            })
        } catch (caught) {
            if (
                extractGraphQlErrorCodes(
                    caught,
                ).includes('SAVED_ROUTE_NOT_FOUND')
            ) {
                setDeleteError(
                    'Die Route existiert nicht mehr.',
                )

                void refetch()
                return
            }

            setDeleteError(DELETE_FAILED_MESSAGE)
        }
    }

    const notFound =
        extractGraphQlErrorCodes(error).includes(
            'SAVED_ROUTE_NOT_FOUND',
        ) ||
        (!loading &&
            !error &&
            data !== undefined &&
            route === null)

    return (
        <div className={styles.page}>
            <div className={listStyles.actions}>
                <Link
                    to="/saved-routes"
                    className={listStyles.primaryLink}
                >
                    <span aria-hidden="true">←</span>
                    {' '}
                    Alle gespeicherten Routen
                </Link>
            </div>

            {loading && !route && (
                <MessageCard>
                    <Loader />

                    <p role="status">
                        Route wird geladen …
                    </p>
                </MessageCard>
            )}

            {notFound && (
                <MessageCard error>
                    <div>
                        <h2>
                            Route nicht gefunden
                        </h2>

                        <p>
                            Diese Route wurde nicht gefunden.
                        </p>

                        <Link
                            to="/saved-routes"
                            className={
                                listStyles.primaryLink
                            }
                        >
                            Zur Routenübersicht
                        </Link>
                    </div>
                </MessageCard>
            )}

            {error && !notFound && !route && (
                <MessageCard error>
                    <div className={listStyles.loadError}>
                        <div>
                            <h2>
                                Route nicht verfügbar
                            </h2>

                            <p>
                                Die Route konnte nicht
                                geladen werden.
                            </p>
                        </div>

                        <button
                            type="button"
                            className={listStyles.button}
                            onClick={() => {
                                void refetch()
                            }}
                        >
                            Erneut versuchen
                        </button>
                    </div>
                </MessageCard>
            )}

            {route && (
                <>
                    <section
                        className={styles.card}
                        aria-labelledby="saved-route-title"
                    >
                        <p className={styles.eyebrow}>
                            Gespeicherte Route
                        </p>

                        <h1 id="saved-route-title">
                            {route.name}
                        </h1>

                        <p className={styles.description}>
                            Übersicht über Strecke,
                            Aktivitätsprofil, Fahrtdaten
                            und Wetterbewertung.
                        </p>

                        <dl className={listStyles.infoGrid}>
                            <div>
                                <dt>Start</dt>
                                <dd>
                                    {route.start.label}
                                </dd>
                            </div>

                            <div>
                                <dt>Ziel</dt>
                                <dd>
                                    {
                                        route.destination
                                            .label
                                    }
                                </dd>
                            </div>

                            <div>
                                <dt>Profil</dt>
                                <dd>
                                    {
                                        route
                                            .activityProfile
                                            .displayName
                                    }
                                </dd>
                            </div>

                            <div>
                                <dt>Abfahrt</dt>
                                <dd>
                                    <time
                                        dateTime={
                                            route.departureTime
                                        }
                                    >
                                        {formatDateTime(
                                            route.departureTime,
                                        )}
                                    </time>
                                </dd>
                            </div>

                            <div>
                                <dt>Ankunft</dt>
                                <dd>
                                    <time
                                        dateTime={
                                            route.arrivalTime
                                        }
                                    >
                                        {formatDateTime(
                                            route.arrivalTime,
                                        )}
                                    </time>
                                </dd>
                            </div>

                            <div>
                                <dt>Distanz</dt>
                                <dd>
                                    {formatDistance(
                                        route.distanceMeters,
                                    )}
                                </dd>
                            </div>

                            <div>
                                <dt>Dauer</dt>
                                <dd>
                                    {formatDuration(
                                        route.durationSeconds,
                                    )}
                                </dd>
                            </div>

                            <div>
                                <dt>Wetterscore</dt>
                                <dd>
                                    {route.weatherScore} / 100
                                </dd>
                            </div>
                        </dl>
                    </section>

                    <section
                        className={listStyles.section}
                        aria-labelledby="saved-route-map-title"
                    >
                        <div
                            className={
                                listStyles.sectionHeader
                            }
                        >
                            <div>
                                <p
                                    className={
                                        listStyles.sectionEyebrow
                                    }
                                >
                                    Streckenverlauf
                                </p>

                                <h2 id="saved-route-map-title">
                                    Route auf der Karte
                                </h2>
                            </div>

                            <span
                                className={
                                    listStyles.routeCount
                                }
                            >
                                Wetterscore{' '}
                                {route.weatherScore}
                            </span>
                        </div>

                        <RouteMap
                            startPlace={startPlace}
                            destinationPlace={
                                destinationPlace
                            }
                            routeSegments={mapSegments}
                        />

                        <div
                            className={listStyles.legend}
                            aria-label="Farblegende des Wetterscores"
                        >
                            <div
                                className={
                                    listStyles.legendBar
                                }
                                aria-hidden="true"
                            />

                            <div
                                className={
                                    listStyles.legendLabels
                                }
                            >
                                <span>
                                    0, ungünstig
                                </span>

                                <span>
                                    100, günstig
                                </span>
                            </div>
                        </div>

                        {deleteError && (
                            <div
                                className={
                                    listStyles.notice
                                }
                                role="alert"
                            >
                                <span
                                    className={
                                        listStyles.noticeIcon
                                    }
                                    aria-hidden="true"
                                >
                                    !
                                </span>

                                <span>
                                    {deleteError}
                                </span>
                            </div>
                        )}

                        {favoriteError && (
                            <p className={listStyles.notice} role="alert">
                                {favoriteError}
                            </p>
                        )}

                        <div className={listStyles.actions}>
                            <FavoriteButton
                                routeId={route.id}
                                routeName={route.name}
                                favorite={route.favorite}
                                withLabel
                                onError={setFavoriteError}
                            />
                            <button
                                type="button"
                                className={
                                    listStyles.deleteButton
                                }
                                disabled={deleting}
                                aria-busy={deleting}
                                onClick={() => {
                                    setDeleteError(undefined)
                                    setConfirmOpen(true)
                                }}
                            >
                                <span aria-hidden="true">
                                    🗑
                                </span>

                                <span>
                                    {deleting
                                        ? 'Route wird gelöscht …'
                                        : 'Route löschen'}
                                </span>
                            </button>
                        </div>
                    </section>
                </>
            )}

            <ConfirmDialog
                open={confirmOpen}
                icon="🗑"
                title="Route wirklich löschen?"
                description={`Die Route „${route?.name ?? ''}“ wird endgültig gelöscht. Das kann nicht rückgängig gemacht werden.`}
                confirmLabel="Endgültig löschen"
                cancelLabel="Abbrechen"
                onConfirm={() => {
                    void handleConfirmDelete()
                }}
                onCancel={() => {
                    setConfirmOpen(false)
                }}
            />
        </div>
    )
}