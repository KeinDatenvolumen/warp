// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
import {useEffect, useState} from 'react'
import {useMutation, useQuery} from '@apollo/client/react'
import {Link, useNavigate} from 'react-router'

import {extractGraphQlErrorCodes} from '../api/graphqlErrors'
import {
    DELETE_SAVED_ROUTE_MUTATION,
    SAVED_ROUTES_QUERY,
} from '../api/savedRoutesApi'
import ConfirmDialog from '../components/ui/ConfirmDialog/ConfirmDialog'
import Loader from '../components/ui/Loader/Loader'
import MessageCard from '../components/ui/MessageCard/MessageCard'
import {formatDate} from '../features/saved-routes/format'
import listStyles from '../features/saved-routes/SavedRoutes.module.css'
import styles from './PagePlaceholder.module.css'
import FavoriteButton from '../features/saved-routes/FavoriteButton'
import type {SavedRouteSummary} from '../types/savedRoute'

const PAGE_SIZE = 20

const DELETE_FAILED_MESSAGE =
    'Die Route konnte nicht gelöscht werden. Bitte versuche es erneut.'

interface RouteToDelete {
    id: string
    name: string
}

export default function SavedRoutesPage() {
    const navigate = useNavigate()
    const [page, setPage] = useState(0)

    const [routeToDelete, setRouteToDelete] =
        useState<RouteToDelete | null>(null)
    const [deleteError, setDeleteError] = useState<string>()
    const [favoriteError, setFavoriteError] = useState<string>()
    const [onlyFavorites, setOnlyFavorites] = useState(false)

    function handleReplan(route: SavedRouteSummary): void {
        navigate('/planner', {
            state: {
                replanRoute: {
                    start: {
                        placeId: `saved-start-${route.id}`,
                        displayName: route.start.label,
                        latitude: route.start.latitude ?? 0,
                        longitude: route.start.longitude ?? 0,
                    },
                    destination: {
                        placeId: `saved-dest-${route.id}`,
                        displayName: route.destination.label,
                        latitude: route.destination.latitude ?? 0,
                        longitude: route.destination.longitude ?? 0,
                    },
                    activityProfile: route.activityProfile.code,
                },
            },
        })
    }

    const {data, loading, error, refetch} = useQuery(SAVED_ROUTES_QUERY, {
        variables: {
            filter: onlyFavorites ? {favorite: true} : undefined,
            page: {page, size: PAGE_SIZE}
        },
        fetchPolicy: 'cache-and-network'
    })

    const [
        deleteSavedRoute,
        {loading: deleting},
    ] = useMutation(DELETE_SAVED_ROUTE_MUTATION)

    const items = data?.savedRoutes.items ?? []

    const totalPages =
        data?.savedRoutes.pageInfo.totalPages ?? 0

    useEffect(() => {
        if (
            data &&
            data.savedRoutes.items.length === 0 &&
            page > 0
        ) {
            setPage((currentPage) =>
                Math.max(0, currentPage - 1),
            )
        }
    }, [data, page])

    function changeFilter(favoritesOnly: boolean): void {
        setOnlyFavorites(favoritesOnly)
        setPage(0)
        setFavoriteError(undefined)
    }

    async function handleConfirmDelete(): Promise<void> {
        if (!routeToDelete || deleting) {
            return
        }

        const target = routeToDelete

        setRouteToDelete(null)
        setDeleteError(undefined)

        try {
            const result = await deleteSavedRoute({
                variables: {
                    id: target.id,
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

            await refetch()
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

    return (
        <div className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>
                    Routenarchiv
                </p>

                <h1>Gespeicherte Routen</h1>

                <p className={styles.description}>
                    Öffne eine gespeicherte Route, um sie
                    erneut auf der Karte zu sehen, oder
                    lösche Routen, die du nicht mehr
                    brauchst.
                </p>
            </section>

<section
    className={listStyles.section}
    aria-labelledby="saved-routes-heading"
>
    <div className={listStyles.sectionHeader}>
        <div>
            <p className={listStyles.sectionEyebrow}>
                Deine Sammlung
            </p>

            <h2 id="saved-routes-heading">
                Routenübersicht
            </h2>
        </div>

        {data && (
            <span
                className={listStyles.routeCount}
                aria-label={`${items.length} Routen auf dieser Seite`}
            >
                {items.length}{' '}
                {items.length === 1
                    ? 'Route'
                    : 'Routen'}
            </span>
        )}
    </div>

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
            <span aria-hidden="true">★</span>{' '}
            Nur Favoriten
        </button>
    </div>

                {deleteError && (
                    <div
                        className={listStyles.notice}
                        role="alert"
                    >
                        <span
                            className={listStyles.noticeIcon}
                            aria-hidden="true"
                        >
                            !
                        </span>

                        <span>{deleteError}</span>
                    </div>
                )}

                {favoriteError && (
                    <p className={listStyles.notice} role="alert">
                        {favoriteError}
                    </p>
                )}

                {loading && !data && (
                    <MessageCard>
                        <Loader />

                        <p role="status">
                            Gespeicherte Routen werden
                            geladen …
                        </p>
                    </MessageCard>
                )}

                {error && !data && (
                    <MessageCard error>
                        <div className={listStyles.loadError}>
                            <div>
                                <h2>
                                    Routen nicht verfügbar
                                </h2>

                                <p>
                                    Die gespeicherten Routen
                                    konnten nicht geladen
                                    werden.
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

{data && items.length === 0 && page === 0 && (
    <div
        className={listStyles.emptyState}
        role="status"
    >
        <span
            className={listStyles.emptyStateIcon}
            aria-hidden="true"
        >
            {onlyFavorites ? '★' : '↗'}
        </span>

        <div>
            {onlyFavorites ? (
                <>
                    <h3>
                        Du hast noch keine Route als Favorit markiert.
                    </h3>

                    <p>
                        Markiere eine gespeicherte Route mit dem Stern,
                        damit sie hier angezeigt wird.
                    </p>
                </>
            ) : (
                <>
                    <h3>
                        Du hast noch keine Route gespeichert.
                    </h3>

                    <p>
                        Plane zuerst eine Route und speichere sie
                        anschließend in deinem Routenarchiv.
                    </p>

                    <Link
                        to="/route-planner"
                        className={listStyles.primaryLink}
                    >
                        Zum Routenplaner
                    </Link>
                </>
            )}
        </div>
    </div>
)}

                {items.length > 0 && (
                    <ul
                        className={listStyles.list}
                        aria-label="Gespeicherte Routen"
                        aria-busy={loading}
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
                                    className={
                                        listStyles.itemLink
                                    }
                                    aria-label={`Route ${route.name} öffnen`}
                                >
                                    <span
                                        className={
                                            listStyles.itemHeader
                                        }
                                    >
                                        <span
                                            className={
                                                listStyles.itemName
                                            }
                                        >
                                            {route.name}
                                        </span>
                                    </span>

                                    <span
                                        className={
                                            listStyles.itemRoute
                                        }
                                    >
                                        <span>
                                            {route.start.label}
                                        </span>

                                        <span
                                            className={
                                                listStyles.routeArrow
                                            }
                                            aria-hidden="true"
                                        >
                                            →
                                        </span>

                                        <span>
                                            {
                                                route
                                                    .destination
                                                    .label
                                            }
                                        </span>
                                    </span>

                                    <span
                                        className={
                                            listStyles.itemMeta
                                        }
                                    >
                                        <span
                                            className={
                                                listStyles.metaItem
                                            }
                                        >
                                            {
                                                route
                                                    .activityProfile
                                                    .displayName
                                            }
                                        </span>

                                        <span
                                            className={
                                                listStyles.metaItem
                                            }
                                        >
                                            Wetterscore{' '}
                                            {
                                                route.weatherScore
                                            }
                                        </span>

                                        <time
                                            className={
                                                listStyles.metaItem
                                            }
                                            dateTime={
                                                route.createdAt
                                            }
                                        >
                                            {formatDate(
                                                route.createdAt,
                                            )}
                                        </time>
                                    </span>
                                </Link>

                          <div className={listStyles.itemActions}>
                              <button
                                  type="button"
                                  className={listStyles.replanButton}
                                  aria-label={`Route ${route.name} neu planen`}
                                  onClick={() => handleReplan(route)}
                              >
                                  <span aria-hidden="true">↻</span>
                                  <span>Neu planen</span>
                              </button>

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
                                  aria-busy={deleting}
                                  onClick={() => {
                                      setDeleteError(undefined)

                                      setRouteToDelete({
                                          id: route.id,
                                          name: route.name,
                                      })
                                  }}
                              >
                                  <span aria-hidden="true">🗑</span>
                                  <span>Löschen</span>
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
                            disabled={
                                page === 0 || loading
                            }
                            onClick={() => {
                                setPage((currentPage) =>
                                    Math.max(
                                        0,
                                        currentPage - 1,
                                    ),
                                )
                            }}
                        >
                            Zurück
                        </button>

                        <span
                            className={
                                listStyles.paginationStatus
                            }
                            aria-live="polite"
                        >
                            Seite {page + 1} von{' '}
                            {totalPages}
                        </span>

                        <button
                            type="button"
                            className={listStyles.button}
                            disabled={
                                page + 1 >= totalPages ||
                                loading
                            }
                            onClick={() => {
                                setPage(
                                    (currentPage) =>
                                        currentPage + 1,
                                )
                            }}
                        >
                            Weiter
                        </button>
                    </nav>
                )}
            </section>

            <ConfirmDialog
                open={routeToDelete !== null}
                icon="🗑"
                title="Route wirklich löschen?"
                description={`Die Route „${routeToDelete?.name ?? ''}“ wird endgültig gelöscht. Das kann nicht rückgängig gemacht werden.`}
                confirmLabel="Endgültig löschen"
                cancelLabel="Abbrechen"
                onConfirm={() => {
                    void handleConfirmDelete()
                }}
                onCancel={() => {
                    setRouteToDelete(null)
                }}
            />
        </div>
    )
}
