import {
    type ChangeEvent,
    type FormEvent,
    useRef,
    useState
} from 'react'
import {Link} from 'react-router'
import {sendPasswordResetEmail} from '../api/passwordResetApi'
import {ApiRequestError} from '../api/restClient'
import {mapPasswordResetSendError} from '../features/password-reset/passwordResetErrorMapping'
import {
    validateForgotPassword,
    type ForgotPasswordFieldErrors,
    type ForgotPasswordValues
} from '../features/password-reset/passwordResetValidation'
import styles from './ForgotPasswordPage.module.css'

const initialValues: ForgotPasswordValues = {
    email: ''
}

export default function ForgotPasswordPage() {
    const [values, setValues] =
        useState<ForgotPasswordValues>(initialValues)

    const [errors, setErrors] =
        useState<ForgotPasswordFieldErrors>({})

    const [formError, setFormError] =
        useState<string>()

    const [successMessage, setSuccessMessage] =
        useState<string>()

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    const isSubmittingRef = useRef(false)
    const emailRef = useRef<HTMLInputElement>(null)

    function handleChange(
        event: ChangeEvent<HTMLInputElement>
    ): void {
        setValues({
            email: event.target.value
        })

        setErrors({})
        setFormError(undefined)
        setSuccessMessage(undefined)
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        if (isSubmittingRef.current) {
            return
        }

        const validationErrors =
            validateForgotPassword(values)

        setErrors(validationErrors)
        setFormError(undefined)
        setSuccessMessage(undefined)

        if (validationErrors.email) {
            emailRef.current?.focus()
            return
        }

        isSubmittingRef.current = true
        setIsSubmitting(true)

        try {
            await sendPasswordResetEmail(
                values.email.trim()
            )

            setSuccessMessage(
                'Falls für diese E-Mail-Adresse ein Benutzerkonto '
                + 'existiert, wurde ein Link zum Zurücksetzen '
                + 'des Passworts versendet.'
            )

            setValues(initialValues)
        } catch (error) {
            if (error instanceof ApiRequestError) {
                setFormError(
                    mapPasswordResetSendError(
                        error.status,
                        error.code,
                        error.message
                    )
                )
            } else {
                setFormError(
                    'Die Passwort-Rücksetzungs-E-Mail konnte '
                    + 'nicht versendet werden. Bitte versuche '
                    + 'es später erneut.'
                )
            }

            emailRef.current?.focus()
        } finally {
            isSubmittingRef.current = false
            setIsSubmitting(false)
        }
    }

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>
                    Passwort-Rücksetzung
                </p>

                <h1>Passwort vergessen?</h1>

                <p className={styles.description}>
                    Gib die E-Mail-Adresse deines Benutzerkontos
                    ein. Wenn ein Konto mit dieser Adresse
                    existiert, senden wir dir einen zeitlich
                    begrenzten Link zum Zurücksetzen deines
                    Passworts.
                </p>

                <form
                    onSubmit={(event) =>
                        void handleSubmit(event)
                    }
                    noValidate
                    aria-busy={isSubmitting}
                >
                    <div className={styles.field}>
                        <label htmlFor="forgot-password-email">
                            E-Mail-Adresse
                        </label>

                        <input
                            ref={emailRef}
                            id="forgot-password-email"
                            name="email"
                            type="email"
                            inputMode="email"
                            autoComplete="email"
                            autoCapitalize="none"
                            spellCheck={false}
                            value={values.email}
                            onChange={handleChange}
                            aria-invalid={Boolean(errors.email)}
                            aria-describedby={
                                errors.email
                                    ? 'forgot-password-email-error'
                                    : undefined
                            }
                        />

                        {errors.email && (
                            <p
                                id="forgot-password-email-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.email}
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

                    {successMessage && (
                        <p
                            className={styles.successMessage}
                            role="status"
                        >
                            {successMessage}
                        </p>
                    )}

                    <button
                        type="submit"
                        className={styles.button}
                        disabled={isSubmitting}
                        aria-busy={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Link wird versendet …'
                            : 'Rücksetzungslink anfordern'}
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