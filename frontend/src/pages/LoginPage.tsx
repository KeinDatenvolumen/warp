import {
    type ChangeEvent,
    type FormEvent,
    useRef,
    useState
} from 'react'
import {Link, Navigate, useLocation} from 'react-router'
import {resolveRedirectTarget} from '../features/auth/authRedirect'
import {
    mapLoginError
} from '../features/auth/loginErrorMapping'
import {
    LOGIN_FIELD_ORDER,
    validateLogin,
    type LoginFieldErrors,
    type LoginValues
} from '../features/auth/loginValidation'
import {useAuth} from '../features/auth/useAuth'
import {useCountdown} from '../features/auth/useCountdown'
import styles from './LoginPage.module.css'

const initialValues: LoginValues = {
    email: '',
    password: ''
}

export default function LoginPage() {
    const {isAuthenticated, login} = useAuth()
    const location = useLocation()
    const {remainingSeconds, start: startCooldown} = useCountdown()

    const [values, setValues] = useState(initialValues)
    const [errors, setErrors] = useState<LoginFieldErrors>({})
    const [formError, setFormError] = useState<string>()
    const [isSubmitting, setIsSubmitting] = useState(false)

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
        event: ChangeEvent<HTMLInputElement>
    ): void {
        const {name, value} = event.target

        setValues((current) => ({...current, [name]: value}))
        setErrors((current) => ({...current, [name]: undefined}))
        setFormError(undefined)
    }

    function focusFirstInvalidField(
        validationErrors: LoginFieldErrors
    ): void {
        const firstInvalid = LOGIN_FIELD_ORDER.find(
            (field) => validationErrors[field]
        )

        if (firstInvalid === 'email') {
            emailRef.current?.focus()
        } else if (firstInvalid === 'password') {
            passwordRef.current?.focus()
        }
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        if (isSubmittingRef.current || isCoolingDown) {
            return
        }

        const validationErrors = validateLogin(values)

        setErrors(validationErrors)
        setFormError(undefined)

        if (Object.keys(validationErrors).length > 0) {
            focusFirstInvalidField(validationErrors)
            return
        }

        isSubmittingRef.current = true
        setIsSubmitting(true)

        try {
            await login({
                email: values.email.trim(),
                password: values.password
            })
        } catch (error) {
            const mapped = mapLoginError(error)

            setFormError(mapped.message)
            setValues((current) => ({...current, password: ''}))

            if (mapped.retryAfterSeconds) {
                startCooldown(mapped.retryAfterSeconds)
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
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Benutzerkonto</p>

                <h1>Anmelden</h1>

                {formError && (
                    <p className={styles.formError} role="alert">
                        {formError}
                    </p>
                )}

                <form
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
                            aria-invalid={Boolean(errors.email)}
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
                            aria-invalid={Boolean(errors.password)}
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

                    <button
                        type="submit"
                        className={styles.button}
                        disabled={isSubmitting || isCoolingDown}
                        aria-busy={isSubmitting}
                    >
                        {submitLabel()}
                    </button>
                </form>

                <p className={styles.registrationPrompt}>
                    Noch keinen Account?{' '}
                    <Link to="/register">Registrieren</Link>
                </p>
            </section>
        </main>
    )
}