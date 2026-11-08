// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-087] (486dab2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-109] (de61707) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-126] (8495425) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: manage session status centrally and implement login form (WARP-006)
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    type ChangeEvent,
    type FormEvent,
    useRef,
    useState,
} from 'react'
import {
    Link,
    Navigate,
    useLocation,
} from 'react-router'

import {
    resolveRedirectTarget,
} from '../features/auth/authRedirect'
import {mapLoginError} from '../features/auth/loginErrorMapping'
import {
    LOGIN_FIELD_ORDER,
    validateLogin,
    type LoginFieldErrors,
    type LoginValues,
} from '../features/auth/loginValidation'
import {useAuth} from '../features/auth/useAuth'
import {useCountdown} from '../features/auth/useCountdown'
import styles from './LoginPage.module.css'

const initialValues: LoginValues = {
    email: '',
    password: '',
}

export default function LoginPage() {
    const {isAuthenticated, login} = useAuth()
    const location = useLocation()

    const {
        remainingSeconds,
        start: startCooldown,
    } = useCountdown()

    const [values, setValues] =
        useState<LoginValues>(initialValues)

    const [errors, setErrors] =
        useState<LoginFieldErrors>({})

    const [formError, setFormError] =
        useState<string>()

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    const isSubmittingRef = useRef(false)
    const emailRef = useRef<HTMLInputElement>(null)
    const passwordRef = useRef<HTMLInputElement>(null)

    if (isAuthenticated) {
        return (
            <Navigate
                to={resolveRedirectTarget(location.state)}
                replace
            />
        )
    }

    const isCoolingDown = remainingSeconds > 0

    function handleChange(
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        const {name, value} = event.target

        setValues((currentValues) => ({
            ...currentValues,
            [name]: value,
        }))

        setErrors((currentErrors) => ({
            ...currentErrors,
            [name]: undefined,
        }))

        setFormError(undefined)
    }

    function focusFirstInvalidField(
        validationErrors: LoginFieldErrors,
    ): void {
        const firstInvalid = LOGIN_FIELD_ORDER.find(
            (field) => validationErrors[field],
        )

        if (firstInvalid === 'email') {
            emailRef.current?.focus()
            return
        }

        if (firstInvalid === 'password') {
            passwordRef.current?.focus()
        }
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        if (
            isSubmittingRef.current ||
            isCoolingDown
        ) {
            return
        }

        const validationErrors =
            validateLogin(values)

        setErrors(validationErrors)
        setFormError(undefined)

        if (
            Object.keys(validationErrors).length > 0
        ) {
            focusFirstInvalidField(validationErrors)
            return
        }

        isSubmittingRef.current = true
        setIsSubmitting(true)

        try {
            await login({
                email: values.email.trim(),
                password: values.password,
            })
        } catch (error) {
            const mapped = mapLoginError(error)

            setFormError(mapped.message)

            setValues((currentValues) => ({
                ...currentValues,  password: '',
            }))

            if (mapped.retryAfterSeconds) {
                startCooldown(
                    mapped.retryAfterSeconds,
                )
            }

            passwordRef.current?.focus()
        } finally {
            isSubmittingRef.current = false
            setIsSubmitting(false)
        }
    }

    function submitLabel(): string {
        if (isSubmitting) {
            return 'Anmeldung läuft …'
        }

        if (isCoolingDown) {
            return `Erneut möglich in ${remainingSeconds} s`
        }

        return 'Anmelden'
    }

    return (
        <div className={styles.page}>
            <section
                className={styles.card}
                aria-labelledby="login-heading"
            >
                <header className={styles.header}>
                    <span
                        className={styles.accountIcon}
                        aria-hidden="true"
                    >
                        ↪
                    </span>

                    <div>
                        <p className={styles.eyebrow}>
                            Benutzerkonto
                        </p>

                        <h1 id="login-heading">
                            Anmelden
                        </h1>
                    </div>
                </header>

                <p className={styles.description}>
                    Melde dich an, um Routen zu planen,
                    zu speichern und später erneut
                    aufzurufen.
                </p>

                {formError && (
                    <div
                        className={styles.formError}
                        role="alert"
                    >
                        <span
                            className={styles.formErrorIcon}
                            aria-hidden="true"
                        >
                            !
                        </span>

                        <span>{formError}</span>
                    </div>
                )}

                <form
                    className={styles.form}
                    onSubmit={handleSubmit}
                    noValidate
                    aria-busy={isSubmitting}
                >
                    <div className={styles.field}>
                        <label htmlFor="login-email">
                            E-Mail-Adresse
                        </label>

                        <input
                            ref={emailRef}
                            id="login-email"
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
                                    ? 'login-email-error'
                                    : undefined
                            }
                        />

                        {errors.email && (
                            <p
                                id="login-email-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.email}
                            </p>
                        )}
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="login-password">
                            Passwort
                        </label>

                        <input
                            ref={passwordRef}
                            id="login-password"
                            name="password"
                            type="password"
                            autoComplete="current-password"
                            value={values.password}
                            onChange={handleChange}
                            aria-invalid={Boolean(
                                errors.password,
                            )}
                            aria-describedby={
                                errors.password
                                    ? 'login-password-error'
                                    : undefined
                            }
                        />

                        {errors.password && (
                            <p
                                id="login-password-error"
                                className={styles.fieldError}
                                role="alert"
                            >
                                {errors.password}
                            </p>
                        )}
                    </div>

                    <div
                        className={
                            styles.forgotPasswordArea
                        }
                    >
                        <Link
                            to="/forgot-password"
                            className={
                                styles.forgotPasswordLink
                            }
                        >
                            Passwort vergessen?
                        </Link>
                    </div>

                    {isCoolingDown && (
                        <div
                            className={
                                styles.cooldownNotice
                            }
                            role="status"
                            aria-live="polite"
                        >
                            <span
                                className={
                                    styles.cooldownIcon
                                }
                                aria-hidden="true"
                            >
                                ⏱
                            </span>

                            <span>
                                Ein neuer Anmeldeversuch
                                ist in {remainingSeconds}{' '}
                                Sekunden möglich.
                            </span>
                        </div>
                    )}

                    <button
                        type="submit"
                        className={styles.button}
                        disabled={
                            isSubmitting ||
                            isCoolingDown
                        }
                        aria-busy={isSubmitting}
                    >
                        {submitLabel()}
                    </button>
                </form>

                <div className={styles.registrationArea}>
                    <p className={styles.registrationPrompt}>
                        Noch keinen Account?
                    </p>

                    <Link
                        to="/register"
                        className={styles.registrationLink}
                    >
                        Registrieren
                    </Link>
                </div>
            </section>
        </div>
    )
}