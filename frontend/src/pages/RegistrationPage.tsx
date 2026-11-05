// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
import {
    type ChangeEvent,
    type FormEvent,
    useState,
} from 'react'
import {Link, useNavigate} from 'react-router'

import {registerUser} from '../api/registrationApi'
import {ApiRequestError} from '../api/restClient'
import ConfirmDialog from '../components/ui/ConfirmDialog/ConfirmDialog'
import {mapRegistrationApiErrors} from '../features/registration/registrationErrorMapping'
import {
    validateRegistration,
    type RegistrationFieldErrors,
    type RegistrationValues,
} from '../features/registration/registrationValidation'
import styles from './RegistrationPage.module.css'

const initialValues: RegistrationValues = {
    name: '',
    email: '',
    password: '',
}

export default function RegistrationPage() {
    const navigate = useNavigate()
    const [values, setValues] =
        useState<RegistrationValues>(initialValues)

    const [errors, setErrors] =
        useState<RegistrationFieldErrors>({})

    const [formError, setFormError] =
        useState<string>()

    const [successMessage, setSuccessMessage] =
        useState<string>()

    const [showVerificationPopup, setShowVerificationPopup] =
        useState(false)

    const [registeredEmail, setRegisteredEmail] =
        useState('')

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    function handleChange(
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        const {name, value} = event.target

        if (name === 'name') {
            setValues((currentValues) => ({
                ...currentValues,
                name: value,
            }))

            setErrors((currentErrors) => ({
                ...currentErrors,
                name: undefined,
            }))
        }

        if (name === 'email') {
            setValues((currentValues) => ({
                ...currentValues,
                email: value,
            }))

            setErrors((currentErrors) => ({
                ...currentErrors,
                email: undefined,
            }))
        }

        if (name === 'password') {
            setValues((currentValues) => ({
                ...currentValues,
                password: value,
            }))

            setErrors((currentErrors) => ({
                ...currentErrors,
                password: undefined,
            }))
        }

        setFormError(undefined)
        setSuccessMessage(undefined)
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        if (isSubmitting) {
            return
        }

        const validationErrors =
            validateRegistration(values)

        setErrors(validationErrors)
        setFormError(undefined)
        setSuccessMessage(undefined)

        if (Object.keys(validationErrors).length > 0) {
            return
        }

        setIsSubmitting(true)

        try {
            const emailValue = values.email.trim()
            const response = await registerUser({
                name: values.name.trim(),
                email: emailValue,
                password: values.password,
            })

            setRegisteredEmail(emailValue)
            setSuccessMessage(response.message)
            setShowVerificationPopup(true)
        } catch (error) {
            if (error instanceof ApiRequestError) {
                const mapped =
                    mapRegistrationApiErrors(
                        error.status,
                        error.code,
                        error.fieldErrors,
                        error.message,
                    )

                setErrors(mapped.fieldErrors)
                setFormError(mapped.formError)
            } else {
                setFormError(
                    'Die Registrierung konnte nicht abgeschlossen werden. Bitte versuche es erneut.',
                )
            }
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className={styles.page}>
            <section
                className={styles.card}
                aria-labelledby="registration-heading"
            >
                <header className={styles.header}>
                    <span
                        className={styles.accountIcon}
                        aria-hidden="true"
                    >
                        +
                    </span>

                    <div>
                        <p className={styles.eyebrow}>
                            Benutzerkonto
                        </p>

                        <h1 id="registration-heading">
                            Registrieren
                        </h1>
                    </div>
                </header>

                <p className={styles.description}>
                    Erstelle ein Benutzerkonto, um
                    wetteroptimierte Routen zu planen,
                    zu speichern und später erneut
                    aufzurufen.
                </p>

                {formError && (
                    <div
                        className={styles.formError}
                        role="alert"
                    >
                        <span
                            className={styles.messageIcon}
                            aria-hidden="true"
                        >
                            !
                        </span>

                        <span>{formError}</span>
                    </div>
                )}

                {successMessage && (
                    <div
                        className={styles.successMessage}
                        role="status"
                    >
                        <span
                            className={styles.successIcon}
                            aria-hidden="true"
                        >
                            ✓
                        </span>

                        <div>
                            <div>{successMessage}</div>
                            <p style={{ margin: '0.35rem 0 0', fontSize: '0.875rem' }}>
                                Bitte prüfe dein E-Mail-Postfach{registeredEmail ? ` (${registeredEmail})` : ''} und klicke auf den Aktivierungslink, um dein Konto zu bestätigen.
                            </p>
                        </div>
                    </div>
                )}

                <form
                    className={styles.form}
                    onSubmit={handleSubmit}
                    noValidate
                    aria-busy={isSubmitting}
                >
                    <div className={styles.field}>
                        <label htmlFor="registration-name">
                            Name
                        </label>

                        <input
                            id="registration-name"
                            name="name"
                            type="text"
                            autoComplete="name"
                            value={values.name}
                            onChange={handleChange}
                            aria-invalid={Boolean(
                                errors.name,
                            )}
                            aria-describedby={
                                errors.name
                                    ? 'registration-name-error'
                                    : undefined
                            }
                        />

                        {errors.name && (
                            <p
                                id="registration-name-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.name}
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="registration-email">
                            E-Mail-Adresse
                        </label>

                        <input
                            id="registration-email"
                            name="email"
                            type="email"
                            inputMode="email"
                            autoComplete="email"
                            autoCapitalize="none"
                            spellCheck={false}
                            value={values.email}
                            onChange={handleChange}
                            aria-invalid={Boolean(
                                errors.email,
                            )}
                            aria-describedby={
                                errors.email
                                    ? 'registration-email-error'
                                    : undefined
                            }
                        />

                        {errors.email && (
                            <p
                                id="registration-email-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.email}
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="registration-password">
                            Passwort
                        </label>

                        <input
                            id="registration-password"
                            name="password"
                            type="password"
                            autoComplete="new-password"
                            value={values.password}
                            onChange={handleChange}
                            aria-invalid={Boolean(
                                errors.password,
                            )}
                            aria-describedby={
                                errors.password
                                    ? 'registration-password-error'
                                    : undefined
                            }
                        />

                        {errors.password && (
                            <p
                                id="registration-password-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.password}
                            </p>
                        )}
                    </div>

                    <button
                        type="submit"
                        className={styles.button}
                        disabled={isSubmitting}
                        aria-busy={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Registrierung läuft …'
                            : 'Registrieren'}
                    </button>
                </form>

                <div className={styles.loginArea}>
                    <p className={styles.loginPrompt}>
                        Bereits einen Account?
                    </p>

                    <Link
                        to="/login"
                        className={styles.loginLink}
                    >
                        Anmelden
                    </Link>
                </div>
            </section>

            <ConfirmDialog
                open={showVerificationPopup}
                icon="✉️"
                title="Bitte E-Mail-Postfach prüfen"
                description={`Wir haben einen Aktivierungslink an ${registeredEmail || 'deine E-Mail-Adresse'} gesendet. Bitte öffne die E-Mail und bestätige deine Adresse, um Routen planen und dein Konto vollständig nutzen zu können.`}
                confirmLabel="Zum Login"
                cancelLabel="Schließen"
                onConfirm={() => navigate('/login')}
                onCancel={() => setShowVerificationPopup(false)}
            />
        </div>
    )
}