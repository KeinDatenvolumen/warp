import {useEffect, useState} from 'react'
import {Link} from 'react-router'
import {useMutation, useQuery} from '@apollo/client/react'
import {
    DELETE_SAVED_ROUTE_MUTATION,
    SAVED_ROUTES_QUERY
} from '../api/savedRoutesApi'
import {extractGraphQlErrorCodes} from '../api/graphqlErrors'
import ConfirmDialog from '../components/ui/ConfirmDialog/ConfirmDialog'
import Loader from '../components/ui/Loader/Loader'
import MessageCard from '../components/ui/MessageCard/MessageCard'
import {formatDate} from '../features/saved-routes/format'
import listStyles from '../features/saved-routes/SavedRoutes.module.css'
import styles from './PagePlaceholder.module.css'
import FavoriteButton from '../features/saved-routes/FavoriteButton'

const PAGE_SIZE = 20

const DELETE_FAILED_MESSAGE =
    'Die Route konnte nicht gelöscht werden. Bitte versuche es erneut.'

interface RouteToDelete {
    id: string
    name: string
}

export default function SavedRoutesPage() {
    const [page, setPage] = useState(0)
    const [routeToDelete, setRouteToDelete] =
        useState<RouteToDelete | null>(null)
    const [deleteError, setDeleteError] = useState<string>()
    const [favoriteError, setFavoriteError] = useState<string>()
    const [onlyFavorites, setOnlyFavorites] = useState(false)

    const {data, loading, error, refetch} = useQuery(SAVED_ROUTES_QUERY, {
        variables: {
            filter: onlyFavorites ? {favorite: true} : undefined,
            page: {page, size: PAGE_SIZE}
        },
        fetchPolicy: 'cache-and-network'
    })

    const [deleteSavedRoute, {loading: deleting}] = useMutation(
        DELETE_SAVED_ROUTE_MUTATION
    )

    // Wurde die letzte Route einer Seite gelöscht, eine Seite zurückgehen.
    useEffect(() => {
        if (data && data.savedRoutes.items.length === 0 && page > 0) {
            setPage(page - 1)
        }
    }, [data, page])

    function changeFilter(favoritesOnly: boolean): void {
        setOnlyFavorites(favoritesOnly)
        setPage(0)
        setFavoriteError(undefined)
    }

    async function handleConfirmDelete(): Promise<void> {
        if (!routeToDelete) {
            return
        }

        const target = routeToDelete
        setRouteToDelete(null)
        setDeleteError(undefined)

        try {
            const result = await deleteSavedRoute({
                variables: {id: target.id},
                update: (cache) => {
                    cache.evict({fieldName: 'savedRoutes'})
                    cache.gc()
                }
            })

            const errors = result.data?.deleteSavedRoute.errors ?? []

            if (errors.length > 0) {
                setDeleteError(errors[0]?.message ?? DELETE_FAILED_MESSAGE)
            }
        } catch (caught) {
            if (
                extractGraphQlErrorCodes(caught).includes(
                    'SAVED_ROUTE_NOT_FOUND'
                )
            ) {
                setDeleteError('Die Route existiert nicht mehr.')
                void refetch()
                return
            }

            setDeleteError(DELETE_FAILED_MESSAGE)
        }
    }

    const items = data?.savedRoutes.items ?? []
    const totalPages = data?.savedRoutes.pageInfo.totalPages ?? 0

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Routenarchiv</p>
                <h1>Gespeicherte Routen</h1>
                <p className={styles.description}>
                    Öffne eine gespeicherte Route, um sie erneut auf der
                    Karte zu sehen, oder lösche Routen, die du nicht mehr
                    brauchst.
                </p>
            </section>

            <div className={listStyles.section}>
                <div
                    className={listStyles.filter}
                    role="group"
                    aria-label="Routen filtern"
                >
                    <button
                        type="button"
                        className={
                            onlyFavorites
                                ? listStyles.filterButton
                                : `${listStyles.filterButton} ${listStyles.filterButtonActive}`
                        }
                        aria-pressed={!onlyFavorites}
                        onClick={() => changeFilter(false)}
                    >
                        Alle
                    </button>
                    <button
                        type="button"
                        className={
                            onlyFavorites
                                ? `${listStyles.filterButton} ${listStyles.filterButtonActive}`
                                : listStyles.filterButton
                        }
                        aria-pressed={onlyFavorites}
                        onClick={() => changeFilter(true)}
                    >
                        ★ Nur Favoriten
                    </button>
                </div>

                {deleteError && (
                    <p className={listStyles.notice} role="alert">
                        {deleteError}
                    </p>
                )}

                {favoriteError && (
                    <p className={listStyles.notice} role="alert">
                        {favoriteError}
                    </p>
                )}

                {loading && !data && (
                    <MessageCard>
                        <Loader/>
                        <p role="status">
                            Gespeicherte Routen werden geladen …
                        </p>
                    </MessageCard>
                )}

                {error && !data && (
                    <MessageCard error>
                        <div>
                            <p>
                                Die gespeicherten Routen konnten nicht
                                geladen werden.
                            </p>
                            <button
                                type="button"
                                className={listStyles.button}
                                onClick={() => void refetch()}
                            >
                                Erneut versuchen
                            </button>
                        </div>
                    </MessageCard>
                )}

                {data && items.length === 0 && page === 0 && (
                    <MessageCard>
                        {onlyFavorites ? (
                            <p>
                                Du hast noch keine Route als Favorit markiert.
                                Markiere eine Route mit dem Stern.
                            </p>
                        ) : (
                            <p>
                                Du hast noch keine Route gespeichert. Plane
                                eine Route im{' '}
                                <Link to="/route-planner">Routenplaner</Link>.
                            </p>
                        )}
                    </MessageCard>
                )}

                {items.length > 0 && (
                    <ul
                        className={listStyles.list}
                        aria-label="Gespeicherte Routen"
                    >
                        {items.map((route) => (
                            <li
                                key={route.id}
                                className={
                                    route.favorite
                                        ? `${listStyles.item} ${listStyles.itemFavorite}`
                                        : listStyles.item
                                }
                            >
                                <Link
                                    to={`/saved-routes/${route.id}`}
                                    className={listStyles.itemLink}
                                >
                                    <span className={listStyles.itemName}>
                                        {route.name}
                                    </span>
                                    <span className={listStyles.itemRoute}>
                                        {route.start.label} →{' '}
                                        {route.destination.label}
                                    </span>
                                    <span className={listStyles.itemMeta}>
                                        {route.activityProfile.displayName} ·
                                        Wetterscore {route.weatherScore} ·{' '}
                                        {formatDate(route.createdAt)}
                                    </span>
                                </Link>

                                <div className={listStyles.itemActions}>
                                    <FavoriteButton
                                        routeId={route.id}
                                        routeName={route.name}
                                        favorite={route.favorite}
                                        onError={setFavoriteError}
                                        onChanged={() => void refetch()}
                                    />
                                    <button
                                        type="button"
                                        className={listStyles.deleteButton}
                                        aria-label={`Route ${route.name} löschen`}
                                        disabled={deleting}
                                        onClick={() =>
                                            setRouteToDelete({
                                                id: route.id,
                                                name: route.name
                                            })
                                        }
                                    >
                                        Löschen
                                    </button>
                                </div>
                            </li>
                        ))}
                    </ul>
                )}

                {totalPages > 1 && (
                    <nav
                        className={listStyles.pagination}
                        aria-label="Seitenwahl"
                    >
                        <button
                            type="button"
                            className={listStyles.button}
                            disabled={page === 0}
                            onClick={() => setPage(page - 1)}
                        >
                            Zurück
                        </button>
                        <span>
                            Seite {page + 1} von {totalPages}
                        </span>
                        <button
                            type="button"
                            className={listStyles.button}
                            disabled={page + 1 >= totalPages}
                            onClick={() => setPage(page + 1)}
                        >
                            Weiter
                        </button>
                    </nav>
                )}
            </div>

            <ConfirmDialog
                open={routeToDelete !== null}
                icon="🗑"
                title="Route wirklich löschen?"
                description={`Die Route „${routeToDelete?.name ?? ''}“ wird endgültig gelöscht. Das kann nicht rückgängig gemacht werden.`}
                confirmLabel="Endgültig löschen"
                onConfirm={() => void handleConfirmDelete()}
                onCancel={() => setRouteToDelete(null)}
            />
        </main>
    )
}