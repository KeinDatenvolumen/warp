// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    type ChangeEvent,
    type FormEvent,
    useRef,
    useState,
} from 'react'
import {Link, useSearchParams} from 'react-router'
import {confirmPasswordReset} from '../api/passwordResetApi'
import {ApiRequestError} from '../api/restClient'
import {mapPasswordResetConfirmError} from '../features/password-reset/passwordResetErrorMapping'
import {
    RESET_PASSWORD_FIELD_ORDER,
    validateResetPassword,
    type ResetPasswordFieldErrors,
    type ResetPasswordValues,
} from '../features/password-reset/passwordResetValidation'
import styles from './ResetPasswordPage.module.css'

const initialValues: ResetPasswordValues = {
    newPassword: '',
    passwordConfirmation: '',
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
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        const {name, value} = event.target

        if (name === 'newPassword') {
            setValues((currentValues) => ({
                ...currentValues,
                newPassword: value,
            }))

            setErrors((currentErrors) => ({
                ...currentErrors,
                newPassword: undefined,
            }))
        }

        if (name === 'passwordConfirmation') {
            setValues((currentValues) => ({
                ...currentValues,
                passwordConfirmation: value,
            }))

            setErrors((currentErrors) => ({
                ...currentErrors,
                passwordConfirmation: undefined,
            }))
        }

        setFormError(undefined)
    }

    function focusFirstInvalidField(
        validationErrors: ResetPasswordFieldErrors,
    ): void {
        const firstInvalid =
            RESET_PASSWORD_FIELD_ORDER.find(
                (field) => validationErrors[field],
            )

        if (firstInvalid === 'newPassword') {
            newPasswordRef.current?.focus()
            return
        }

        if (
            firstInvalid ===
            'passwordConfirmation'
        ) {
            passwordConfirmationRef.current?.focus()
        }
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        if (
            isSubmittingRef.current ||
            !token
        ) {
            return
        }

        const validationErrors =
            validateResetPassword(values)

        setErrors(validationErrors)
        setFormError(undefined)

        if (
            Object.keys(validationErrors).length > 0
        ) {
            focusFirstInvalidField(
                validationErrors,
            )
            return
        }

        isSubmittingRef.current = true
        setIsSubmitting(true)

        let shouldFocusPassword = false

        try {
            await confirmPasswordReset(
                token,
                values.newPassword,
            )

            setValues(initialValues)
            setIsSuccessful(true)
        } catch (error) {
            if (error instanceof ApiRequestError) {
                setFormError(
                    mapPasswordResetConfirmError(
                        error.status,
                        error.code,
                        error.message,
                    ),
                )
            } else {
                setFormError(
                    'Das Passwort konnte nicht zurückgesetzt werden. Bitte versuche es erneut.',
                )
            }

            setValues({
                newPassword: '',
                passwordConfirmation: '',
            })

            shouldFocusPassword = true
        } finally {
            isSubmittingRef.current = false
            setIsSubmitting(false)

            if (shouldFocusPassword) {
                window.setTimeout(() => {
                    newPasswordRef.current?.focus()
                }, 0)
            }
        }
    }

    if (!token) {
        return (
            <div className={styles.page}>
                <section className={styles.card}>
                    <p className={styles.eyebrow}>
                        Passwort-Rücksetzung
                    </p>

                    <h1>
                        Rücksetzungslink unvollständig
                    </h1>

                    <div
                        className={styles.formError}
                        role="alert"
                    >
                        <span>
                            Es wurde kein Rücksetzungstoken
                            übergeben. Bitte verwende den
                            vollständigen Link aus deiner
                            E-Mail.
                        </span>
                    </div>

                    <div className={styles.footerLinks}>
                        <Link to="/forgot-password">
                            Neuen Rücksetzungslink anfordern
                        </Link>

                        <Link to="/login">
                            Zurück zum Login
                        </Link>
                    </div>
                </section>
            </div>
        )
    }

    if (isSuccessful) {
        return (
            <div className={styles.page}>
                <section className={styles.card}>
                    <p className={styles.eyebrow}>
                        Passwort-Rücksetzung
                    </p>

                    <h1>
                        Passwort erfolgreich geändert
                    </h1>

                    <div
                        className={styles.successMessage}
                        role="status"
                    >
                        <span>
                            Dein neues Passwort wurde
                            gespeichert. Du kannst dich
                            jetzt damit anmelden.
                        </span>
                    </div>

                    <Link
                        to="/login"
                        className={styles.button}
                    >
                        Zum Login
                    </Link>
                </section>
            </div>
        )
    }

    return (
        <div className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>
                    Passwort-Rücksetzung
                </p>

                <h1>
                    Neues Passwort vergeben
                </h1>

                <p className={styles.description}>
                    Das neue Passwort muss mindestens zwölf
                    Zeichen lang sein. Der Rücksetzungslink
                    kann nur einmal verwendet werden.
                </p>

                <form
                    onSubmit={handleSubmit}
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
                            aria-invalid={Boolean(
                                errors.newPassword,
                            )}
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
                        <label htmlFor="reset-password-confirmation">
                            Neues Passwort wiederholen
                        </label>

                        <input
                            ref={passwordConfirmationRef}
                            id="reset-password-confirmation"
                            name="passwordConfirmation"
                            type="password"
                            autoComplete="new-password"
                            value={
                                values.passwordConfirmation
                            }
                            onChange={handleChange}
                            aria-invalid={Boolean(
                                errors.passwordConfirmation,
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
                                {
                                    errors.passwordConfirmation
                                }
                            </p>
                        )}
                    </div>

                    {formError && (
                        <div
                            className={styles.formError}
                            role="alert"
                        >
                            <span>{formError}</span>
                        </div>
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
        </div>
    )
}