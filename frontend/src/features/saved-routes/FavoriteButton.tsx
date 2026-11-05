// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Favoriten-Funktion für gespeicherte Routen (WARP-022)
import {useMutation} from '@apollo/client/react'
import {extractGraphQlErrorCodes} from '../../api/graphqlErrors'
import {UPDATE_SAVED_ROUTE_MUTATION} from '../../api/savedRoutesApi'
import styles from './SavedRoutes.module.css'

const FAVORITE_FAILED_MESSAGE =
    'Der Favoritenstatus konnte nicht geändert werden. Bitte versuche es erneut.'

interface FavoriteButtonProps {
    routeId: string
    routeName: string
    favorite: boolean
    /** Zeigt zusätzlich einen Text neben dem Stern an. */
    withLabel?: boolean
    /** Wird mit einer Fehlermeldung bzw. mit undefined (Fehler zurücksetzen) aufgerufen. */
    onError?: (message: string | undefined) => void
    /** Wird nach erfolgreicher Änderung aufgerufen, z. B. um die Liste neu zu laden. */
    onChanged?: () => void
}

export default function FavoriteButton({
                                           routeId,
                                           routeName,
                                           favorite,
                                           withLabel = false,
                                           onError,
                                           onChanged
                                       }: FavoriteButtonProps) {
    const [updateSavedRoute, {loading}] = useMutation(
        UPDATE_SAVED_ROUTE_MUTATION
    )

    async function handleClick(): Promise<void> {
        onError?.(undefined)

        try {
            const result = await updateSavedRoute({
                variables: {id: routeId, input: {favorite: !favorite}}
            })

            const errors = result.data?.updateSavedRoute?.errors ?? []

            if (errors.length > 0) {
                onError?.(errors[0]?.message ?? FAVORITE_FAILED_MESSAGE)
                return
            }

            onChanged?.()
        } catch (caught) {
            if (
                extractGraphQlErrorCodes(caught).includes(
                    'SAVED_ROUTE_NOT_FOUND'
                )
            ) {
                onError?.('Die Route existiert nicht mehr.')
                return
            }

            onError?.(FAVORITE_FAILED_MESSAGE)
        }
    }

    const className = favorite
        ? `${styles.favoriteButton} ${styles.favoriteButtonActive}`
        : styles.favoriteButton

    return (
        <button
            type="button"
            className={className}
            aria-pressed={favorite}
            aria-label={`Route ${routeName} als Favorit markieren`}
            disabled={loading}
            onClick={() => void handleClick()}
        >
            <span aria-hidden="true">{favorite ? '★' : '☆'}</span>
            {withLabel && (
                <span aria-hidden="true">
                    {favorite ? ' Favorit' : ' Als Favorit markieren'}
                </span>
            )}
        </button>
    )
}