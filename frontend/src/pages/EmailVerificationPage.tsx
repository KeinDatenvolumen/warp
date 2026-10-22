import {
    type ChangeEvent,
    type FormEvent,
    useEffect,
    useRef,
    useState
} from 'react'
import {Link, useSearchParams} from 'react-router'
import {confirmEmail, resendVerificationEmail} from '../api/emailVerificationApi'
import {ApiRequestError} from '../api/restClient'
import Loader from '../components/ui/Loader/Loader'
import {
    mapConfirmError,
    mapResendError
} from '../features/email-verification/emailVerificationErrorMapping'
import {validateResendEmail} from '../features/email-verification/emailVerificationValidation'
import styles from './EmailVerificationPage.module.css'

type VerificationState =
    | {status: 'verifying'}
    | {status: 'success'}
    | {status: 'error'; message: string}
    | {status: 'no_token'}

export default function EmailVerificationPage() {
    const [searchParams] = useSearchParams()
    const token = searchParams.get('token')?.trim() || ''

    const [state, setState] = useState<VerificationState>(() =>
        token ? {status: 'verifying'} : {status: 'no_token'}
    )

    const verificationStartedRef = useRef<string | null>(null)

    const [resendEmailInput, setResendEmailInput] = useState('')
    const [resendEmailError, setResendEmailError] = useState<string>()
    const [resendFormError, setResendFormError] = useState<string>()
    const [resendSuccessMessage, setResendSuccessMessage] = useState<string>()
    const [isResending, setIsResending] = useState(false)

    useEffect(() => {
        if (!token) {
            setState({status: 'no_token'})
            return
        }

        if (verificationStartedRef.current === token) {
            return
        }
        verificationStartedRef.current = token

        confirmEmail(token)
            .then(() => {
                setState({status: 'success'})
            })
            .catch((error) => {
                const message =
                    error instanceof ApiRequestError
                        ? mapConfirmError(
                              error.status,
                              error.code,
                              error.message
                          )
                        : 'Die E-Mail-Adresse konnte nicht bestätigt werden. Bitte versuche es erneut.'

                setState({status: 'error', message})
            })
    }, [token])

    function handleResendEmailChange(
        event: ChangeEvent<HTMLInputElement>
    ): void {
        setResendEmailInput(event.target.value)
        setResendEmailError(undefined)
        setResendFormError(undefined)
        setResendSuccessMessage(undefined)
    }

    async function handleResendSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        if (isResending) {
            return
        }

        const validationError = validateResendEmail(resendEmailInput)
        if (validationError) {
            setResendEmailError(validationError)
            return
        }

        setIsResending(true)
        setResendEmailError(undefined)
        setResendFormError(undefined)
        setResendSuccessMessage(undefined)

        try {
            await resendVerificationEmail(resendEmailInput.trim())
            setResendSuccessMessage(
                'Falls ein Benutzerkonto mit dieser E-Mail-Adresse existiert, wurde eine neue Bestätigungs-E-Mail versendet.'
            )
            setResendEmailInput('')
        } catch (error) {
            if (error instanceof ApiRequestError) {
                setResendFormError(
                    mapResendError(error.status, error.code, error.message)
                )
            } else {
                setResendFormError(
                    'Die Bestätigungs-E-Mail konnte nicht erneut gesendet werden. Bitte versuche es erneut.'
                )
            }
        } finally {
            setIsResending(false)
        }
    }

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>E-Mail-Verifikation</p>

                {state.status === 'verifying' && (
                    <div className={styles.loaderWrapper}>
                        <Loader />
                        <h1>E-Mail-Adresse wird bestätigt …</h1>
                        <p className={styles.description}>
                            Bitte habe einen kurzen Moment Geduld.
                        </p>
                    </div>
                )}

                {state.status === 'success' && (
                    <div>
                        <h1>E-Mail-Adresse bestätigt</h1>
                        <p className={styles.description}>
                            Deine E-Mail-Adresse wurde erfolgreich bestätigt.
                            Dein Konto ist jetzt vollständig aktiviert und du kannst
                            dich anmelden.
                        </p>
                        <Link to="/login" className={styles.button}>
                            Zum Login
                        </Link>
                    </div>
                )}

                {(state.status === 'error' || state.status === 'no_token') && (
                    <div>
                        <h1>
                            {state.status === 'error'
                                ? 'Bestätigung fehlgeschlagen'
                                : 'Bestätigungslink unvollständig'}
                        </h1>

                        <p className={styles.formError} role="alert">
                            {state.status === 'error'
                                ? state.message
                                : 'Es wurde kein Bestätigungstoken übergeben. Bitte verwende den vollständigen Link aus deiner E-Mail.'}
                        </p>

                        <div className={styles.resendSection}>
                            <h2>Bestätigungs-E-Mail erneut anfordern</h2>
                            <p className={styles.description}>
                                Gib deine E-Mail-Adresse ein, um einen neuen
                                Bestätigungslink zu erhalten.
                            </p>

                            <form onSubmit={(e) => void handleResendSubmit(e)} noValidate>
                                <div className={styles.field}>
                                    <label htmlFor="resend-email">
                                        E-Mail-Adresse
                                    </label>
                                    <input
                                        id="resend-email"
                                        name="email"
                                        type="email"
                                        autoComplete="email"
                                        value={resendEmailInput}
                                        onChange={handleResendEmailChange}
                                        aria-invalid={resendEmailError !== undefined}
                                        aria-describedby={
                                            resendEmailError
                                                ? 'resend-email-error'
                                                : undefined
                                        }
                                    />
                                    {resendEmailError && (
                                        <p
                                            id="resend-email-error"
                                            className={styles.fieldError}
                                            role="alert"
                                        >
                                            {resendEmailError}
                                        </p>
                                    )}
                                </div>

                                {resendFormError && (
                                    <p className={styles.formError} role="alert">
                                        {resendFormError}
                                    </p>
                                )}

                                {resendSuccessMessage && (
                                    <p
                                        className={styles.successMessage}
                                        role="status"
                                    >
                                        {resendSuccessMessage}
                                    </p>
                                )}

                                <button
                                    type="submit"
                                    className={styles.button}
                                    disabled={isResending}
                                >
                                    {isResending
                                        ? 'Wird versendet …'
                                        : 'Neue Bestätigungs-E-Mail senden'}
                                </button>
                            </form>
                        </div>

                        <div className={styles.footerLinks}>
                            <Link to="/login">Zurück zum Login</Link>
                        </div>
                    </div>
                )}
            </section>
        </main>
    )
}
