import {useMemo, useState} from 'react'
import {Link, useNavigate, useParams} from 'react-router'
import {useMutation, useQuery} from '@apollo/client/react'
import {
    DELETE_SAVED_ROUTE_MUTATION,
    SAVED_ROUTE_QUERY
} from '../api/savedRoutesApi'
import {extractGraphQlErrorCodes} from '../api/graphqlErrors'
import RouteMap from '../components/RouteMap/RouteMap'
import ConfirmDialog from '../components/ui/ConfirmDialog/ConfirmDialog'
import Loader from '../components/ui/Loader/Loader'
import MessageCard from '../components/ui/MessageCard/MessageCard'
import {
    formatDateTime,
    formatDistance,
    formatDuration
} from '../features/saved-routes/format'
import {buildRouteMapSegments} from '../features/saved-routes/routeGeometry'
import listStyles from '../features/saved-routes/SavedRoutes.module.css'
import type {RoutePlace} from '../types/savedRoute'
import type {Place} from '../types/searchPlaces'
import styles from './PagePlaceholder.module.css'

function toPlace(id: string, place: RoutePlace): Place {
    return {
        placeId: id,
        displayName: place.label,
        latitude: place.latitude,
        longitude: place.longitude
    }
}

export default function SavedRouteDetailPage() {
    const {routeId} = useParams()
    const navigate = useNavigate()
    const [confirmOpen, setConfirmOpen] = useState(false)
    const [deleteError, setDeleteError] = useState<string>()

    const {data, loading, error} = useQuery(SAVED_ROUTE_QUERY, {
        variables: {id: routeId ?? ''},
        skip: !routeId,
        fetchPolicy: 'cache-and-network'
    })

    const [deleteSavedRoute, {loading: deleting}] = useMutation(
        DELETE_SAVED_ROUTE_MUTATION
    )

    const route = data?.savedRoute ?? null

    const mapSegments = useMemo(
        () => (route ? buildRouteMapSegments(route) : []),
        [route]
    )
    const startPlace = useMemo(
        () => (route ? toPlace('saved-start', route.start) : null),
        [route]
    )
    const destinationPlace = useMemo(
        () => (route ? toPlace('saved-destination', route.destination) : null),
        [route]
    )

    async function handleConfirmDelete(): Promise<void> {
        setConfirmOpen(false)
        setDeleteError(undefined)

        if (!routeId) {
            return
        }

        try {
            const result = await deleteSavedRoute({
                variables: {id: routeId},
                update: (cache) => {
                    cache.evict({fieldName: 'savedRoutes'})
                    cache.gc()
                }
            })

            const errors = result.data?.deleteSavedRoute.errors ?? []

            if (errors.length > 0) {
                setDeleteError(
                    errors[0]?.message ?? 'Die Route konnte nicht gelöscht werden.'
                )
                return
            }

            navigate('/saved-routes', {replace: true})
        } catch {
            setDeleteError(
                'Die Route konnte nicht gelöscht werden. Bitte versuche es erneut.'
            )
        }
    }

    const notFound =
        extractGraphQlErrorCodes(error).includes('SAVED_ROUTE_NOT_FOUND') ||
        (!loading && !error && data !== undefined && route === null)

    return (
        <main className={styles.page}>
            <p>
                <Link to="/saved-routes">← Alle gespeicherten Routen</Link>
            </p>

            {loading && !route && (
                <MessageCard>
                    <Loader/>
                    <p role="status">Route wird geladen …</p>
                </MessageCard>
            )}

            {notFound && (
                <MessageCard error>
                    <p>Diese Route wurde nicht gefunden.</p>
                </MessageCard>
            )}

            {error && !notFound && !route && (
                <MessageCard error>
                    <p>Die Route konnte nicht geladen werden.</p>
                </MessageCard>
            )}

            {route && (
                <>
                    <section className={styles.card}>
                        <p className={styles.eyebrow}>Gespeicherte Route</p>
                        <h1>{route.name}</h1>

                        <dl className={listStyles.infoGrid}>
                            <div>
                                <dt>Start</dt>
                                <dd>{route.start.label}</dd>
                            </div>
                            <div>
                                <dt>Ziel</dt>
                                <dd>{route.destination.label}</dd>
                            </div>
                            <div>
                                <dt>Profil</dt>
                                <dd>{route.activityProfile.displayName}</dd>
                            </div>
                            <div>
                                <dt>Abfahrt</dt>
                                <dd>{formatDateTime(route.departureTime)}</dd>
                            </div>
                            <div>
                                <dt>Ankunft</dt>
                                <dd>{formatDateTime(route.arrivalTime)}</dd>
                            </div>
                            <div>
                                <dt>Distanz</dt>
                                <dd>{formatDistance(route.distanceMeters)}</dd>
                            </div>
                            <div>
                                <dt>Dauer</dt>
                                <dd>{formatDuration(route.durationSeconds)}</dd>
                            </div>
                            <div>
                                <dt>Wetterscore</dt>
                                <dd>{route.weatherScore} / 100</dd>
                            </div>
                        </dl>
                    </section>

                    <div className={listStyles.section}>
                        <RouteMap
                            startPlace={startPlace}
                            destinationPlace={destinationPlace}
                            routeSegments={mapSegments}
                        />

                        <div className={listStyles.legend}>
                            <div className={listStyles.legendBar}/>
                            <div className={listStyles.legendLabels}>
                                <span>Wetterscore 0 (ungünstig)</span>
                                <span>100 (günstig)</span>
                            </div>
                        </div>

                        {deleteError && (
                            <p className={listStyles.notice} role="alert">
                                {deleteError}
                            </p>
                        )}

                        <div className={listStyles.actions}>
                            <button
                                type="button"
                                className={listStyles.deleteButton}
                                disabled={deleting}
                                onClick={() => setConfirmOpen(true)}
                            >
                                Route löschen
                            </button>
                        </div>
                    </div>
                </>
            )}

            <ConfirmDialog
                open={confirmOpen}
                icon="🗑"
                title="Route wirklich löschen?"
                description={`Die Route „${route?.name ?? ''}“ wird endgültig gelöscht. Das kann nicht rückgängig gemacht werden.`}
                confirmLabel="Endgültig löschen"
                onConfirm={() => void handleConfirmDelete()}
                onCancel={() => setConfirmOpen(false)}
            />
        </main>
    )
}