import {
    type ChangeEvent,
    type FormEvent,
    useRef,
    useState
} from 'react'
import {Link, useSearchParams} from 'react-router'
import {confirmPasswordReset} from '../api/passwordResetApi'
import {ApiRequestError} from '../api/restClient'
import {mapPasswordResetConfirmError} from '../features/password-reset/passwordResetErrorMapping'
import {
    RESET_PASSWORD_FIELD_ORDER,
    validateResetPassword,
    type ResetPasswordFieldErrors,
    type ResetPasswordValues
} from '../features/password-reset/passwordResetValidation'
import styles from './ResetPasswordPage.module.css'

const initialValues: ResetPasswordValues = {
    newPassword: '',
    passwordConfirmation: ''
}

export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams()
    const token = searchParams.get('token')?.trim() || ''

    const [values, setValues] =
        useState<ResetPasswordValues>(initialValues)

    const [errors, setErrors] =
        useState<ResetPasswordFieldErrors>({})

    const [formError, setFormError] =
        useState<string>()

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    const [isSuccessful, setIsSuccessful] =
        useState(false)

    const isSubmittingRef = useRef(false)

    const newPasswordRef =
        useRef<HTMLInputElement>(null)

    const passwordConfirmationRef =
        useRef<HTMLInputElement>(null)

    function handleChange(
        event: ChangeEvent<HTMLInputElement>
    ): void {
        const {name, value} = event.target

        setValues((current) => ({
            ...current,
            [name]: value
        }))

        setErrors((current) => ({
            ...current,
            [name]: undefined
        }))

        setFormError(undefined)
    }

    function focusFirstInvalidField(
        validationErrors: ResetPasswordFieldErrors
    ): void {
        const firstInvalid =
            RESET_PASSWORD_FIELD_ORDER.find(
                (field) => validationErrors[field]
            )

        if (firstInvalid === 'newPassword') {
            newPasswordRef.current?.focus()
        } else if (
            firstInvalid === 'passwordConfirmation'
        ) {
            passwordConfirmationRef.current?.focus()
        }
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        if (isSubmittingRef.current || !token) {
            return
        }

        const validationErrors =
            validateResetPassword(values)

        setErrors(validationErrors)
        setFormError(undefined)

        if (Object.keys(validationErrors).length > 0) {
            focusFirstInvalidField(validationErrors)
            return
        }

        isSubmittingRef.current = true
        setIsSubmitting(true)

        try {
            await confirmPasswordReset(
                token,
                values.newPassword
            )

            setValues(initialValues)
            setIsSuccessful(true)
        } catch (error) {
            if (error instanceof ApiRequestError) {
                setFormError(
                    mapPasswordResetConfirmError(
                        error.status,
                        error.code,
                        error.message
                    )
                )
            } else {
                setFormError(
                    'Das Passwort konnte nicht zurückgesetzt '
                    + 'werden. Bitte versuche es erneut.'
                )
            }

            setValues((current) => ({
                ...current,
                newPassword: '',
                passwordConfirmation: ''
            }))

            newPasswordRef.current?.focus()
        } finally {
            isSubmittingRef.current = false
            setIsSubmitting(false)
        }
    }

    if (!token) {
        return (
            <main className={styles.page}>
                <section className={styles.card}>
                    <p className={styles.eyebrow}>
                        Passwort-Rücksetzung
                    </p>

                    <h1>Rücksetzungslink unvollständig</h1>

                    <p
                        className={styles.formError}
                        role="alert"
                    >
                        Es wurde kein Rücksetzungstoken
                        übergeben. Bitte verwende den
                        vollständigen Link aus deiner E-Mail.
                    </p>

                    <div className={styles.footerLinks}>
                        <Link to="/forgot-password">
                            Neuen Rücksetzungslink anfordern
                        </Link>

                        <Link to="/login">
                            Zurück zum Login
                        </Link>
                    </div>
                </section>
            </main>
        )
    }

    if (isSuccessful) {
        return (
            <main className={styles.page}>
                <section className={styles.card}>
                    <p className={styles.eyebrow}>
                        Passwort-Rücksetzung
                    </p>

                    <h1>Passwort erfolgreich geändert</h1>

                    <p
                        className={styles.successMessage}
                        role="status"
                    >
                        Dein neues Passwort wurde gespeichert.
                        Du kannst dich jetzt damit anmelden.
                    </p>

                    <Link
                        to="/login"
                        className={styles.button}
                    >
                        Zum Login
                    </Link>
                </section>
            </main>
        )
    }

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>
                    Passwort-Rücksetzung
                </p>

                <h1>Neues Passwort vergeben</h1>

                <p className={styles.description}>
                    Das neue Passwort muss mindestens zwölf
                    Zeichen lang sein. Der Rücksetzungslink
                    kann nur einmal verwendet werden.
                </p>

                <form
                    onSubmit={(event) =>
                        void handleSubmit(event)
                    }
                    noValidate
                    aria-busy={isSubmitting}
                >
                    <div className={styles.field}>
                        <label htmlFor="reset-new-password">
                            Neues Passwort
                        </label>

                        <input
                            ref={newPasswordRef}
                            id="reset-new-password"
                            name="newPassword"
                            type="password"
                            autoComplete="new-password"
                            value={values.newPassword}
                            onChange={handleChange}
                            aria-invalid={
                                Boolean(errors.newPassword)
                            }
                            aria-describedby={
                                errors.newPassword
                                    ? 'reset-new-password-error'
                                    : 'reset-password-requirements'
                            }
                        />

                        <p
                            id="reset-password-requirements"
                            className={styles.hint}
                        >
                            Mindestens zwölf Zeichen
                        </p>

                        {errors.newPassword && (
                            <p
                                id="reset-new-password-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.newPassword}
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label
                            htmlFor="reset-password-confirmation"
                        >
                            Neues Passwort wiederholen
                        </label>

                        <input
                            ref={passwordConfirmationRef}
                            id="reset-password-confirmation"
                            name="passwordConfirmation"
                            type="password"
                            autoComplete="new-password"
                            value={values.passwordConfirmation}
                            onChange={handleChange}
                            aria-invalid={Boolean(
                                errors.passwordConfirmation
                            )}
                            aria-describedby={
                                errors.passwordConfirmation
                                    ? 'reset-password-confirmation-error'
                                    : undefined
                            }
                        />

                        {errors.passwordConfirmation && (
                            <p
                                id="reset-password-confirmation-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.passwordConfirmation}
                            </p>
                        )}
                    </div>

                    {formError && (
                        <p
                            className={styles.formError}
                            role="alert"
                        >
                            {formError}
                        </p>
                    )}

                    <button
                        type="submit"
                        className={styles.button}
                        disabled={isSubmitting}
                        aria-busy={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Passwort wird gespeichert …'
                            : 'Neues Passwort speichern'}
                    </button>
                </form>

                <div className={styles.footerLinks}>
                    <Link to="/login">
                        Zurück zum Login
                    </Link>
                </div>
            </section>
        </main>
    )
}