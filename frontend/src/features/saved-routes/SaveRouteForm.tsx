// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
import {useState, type FormEvent} from 'react'
import {Link} from 'react-router'
import {useMutation} from '@apollo/client/react'
import {extractGraphQlErrorCodes} from '../../api/graphqlErrors'
import {SAVE_ROUTE_MUTATION} from '../../api/savedRoutesApi'
import {validateRouteName} from './routeNameValidation'
import styles from './SavedRoutes.module.css'

const GENERIC_ERROR =
    'Die Route konnte nicht gespeichert werden. Bitte versuche es erneut.'

function messageForError(error: unknown): string {
    const codes = extractGraphQlErrorCodes(error)

    if (codes.includes('EMAIL_NOT_VERIFIED')) {
        return 'Bitte bestätige zuerst deine E-Mail-Adresse, um Routen zu speichern.'
    }

    if (codes.includes('SESSION_INVALID') || codes.includes('NOT_AUTHENTICATED')) {
        return 'Bitte melde dich erneut an.'
    }

    return GENERIC_ERROR
}

interface SaveRouteFormProps {
    routeCalculationId: string
    weatherEvaluationId: string
}

export default function SaveRouteForm({
                                          routeCalculationId,
                                          weatherEvaluationId
                                      }: SaveRouteFormProps) {
    const [name, setName] = useState('')
    const [nameError, setNameError] = useState<string>()
    const [formError, setFormError] = useState<string>()
    const [savedName, setSavedName] = useState<string>()

    const [saveRoute, {loading}] = useMutation(SAVE_ROUTE_MUTATION)

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        if (loading) {
            return
        }

        setFormError(undefined)
        setSavedName(undefined)

        const validationError = validateRouteName(name)

        if (validationError) {
            setNameError(validationError)
            return
        }

        setNameError(undefined)

        try {
            const result = await saveRoute({
                variables: {
                    input: {
                        name: name.trim(),
                        routeCalculationId,
                        weatherEvaluationId,
                        favorite: false
                    }
                },
                update: (cache) => {
                    cache.evict({fieldName: 'savedRoutes'})
                    cache.gc()
                }
            })

            const payload = result.data?.saveRoute
            const fieldError = payload?.errors.find(
                (error) => error.field === 'name'
            )

            if (fieldError) {
                setNameError(fieldError.message)
                return
            }

            if (payload && payload.errors.length > 0) {
                setFormError(payload.errors[0]?.message ?? GENERIC_ERROR)
                return
            }

            if (payload?.route) {
                setSavedName(payload.route.name)
                setName('')
                return
            }

            setFormError(GENERIC_ERROR)
        } catch (caught) {
            setFormError(messageForError(caught))
        }
    }

    return (
        <form
            className={styles.form}
            onSubmit={(event) => void handleSubmit(event)}
            noValidate
        >
            <div className={styles.field}>
                <label htmlFor="route-name">Name der Route</label>
                <input
                    id="route-name"
                    name="routeName"
                    type="text"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    aria-invalid={nameError !== undefined}
                    aria-describedby={nameError ? 'route-name-error' : undefined}
                    autoComplete="off"
                />
                {nameError && (
                    <span
                        id="route-name-error"
                        className={styles.fieldError}
                        role="alert"
                    >
                        {nameError}
                    </span>
                )}
            </div>

            {formError && (
                <p className={styles.notice} role="alert">
                    {formError}
                </p>
            )}

            {savedName && (
                <p className={styles.success} role="status">
                    Die Route „{savedName}“ wurde gespeichert.{' '}
                    <Link to="/saved-routes">Zu den gespeicherten Routen</Link>
                </p>
            )}

            <div className={styles.actions}>
                <button
                    type="submit"
                    className={styles.button}
                    disabled={loading}
                    aria-busy={loading}
                >
                    {loading ? 'Wird gespeichert …' : 'Route speichern'}
                </button>
            </div>
        </form>
    )
}