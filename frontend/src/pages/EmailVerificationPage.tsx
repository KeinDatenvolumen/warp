import {
    type ChangeEvent,
    type FormEvent,
    useEffect,
    useRef,
    useState,
} from 'react'
import {Link, useSearchParams} from 'react-router'

import {
    confirmEmail,
    resendVerificationEmail,
} from '../api/emailVerificationApi'
import {ApiRequestError} from '../api/restClient'
import Loader from '../components/ui/Loader/Loader'
import {
    mapConfirmError,
    mapResendError,
} from '../features/email-verification/emailVerificationErrorMapping'
import {
    validateResendEmail,
} from '../features/email-verification/emailVerificationValidation'
import styles from './EmailVerificationPage.module.css'

type VerificationState =
    | {status: 'verifying'}
    | {status: 'success'}
    | {status: 'error'; message: string}
    | {status: 'no_token'}

export default function EmailVerificationPage() {
    const [searchParams] = useSearchParams()

    const token =
        searchParams.get('token')?.trim() || ''

    const [state, setState] =
        useState<VerificationState>(() =>
            token
                ? {status: 'verifying'}
                : {status: 'no_token'},
        )

    const verificationStartedRef =
        useRef<string | null>(null)

    const [resendEmailInput, setResendEmailInput] =
        useState('')

    const [resendEmailError, setResendEmailError] =
        useState<string>()

    const [resendFormError, setResendFormError] =
        useState<string>()

    const [
        resendSuccessMessage,
        setResendSuccessMessage,
    ] = useState<string>()

    const [isResending, setIsResending] =
        useState(false)

    useEffect(() => {
        if (!token) {
            setState({
                status: 'no_token',
            })
            return
        }

        if (
            verificationStartedRef.current === token
        ) {
            return
        }

        verificationStartedRef.current = token

        void confirmEmail(token)
            .then(() => {
                setState({
                    status: 'success',
                })
            })
            .catch((error: unknown) => {
                const message =
                    error instanceof ApiRequestError
                        ? mapConfirmError(
                            error.status,
                            error.code,
                            error.message,
                        )
                        : 'Die E-Mail-Adresse konnte nicht bestätigt werden. Bitte versuche es erneut.'

                setState({
                    status: 'error',
                    message,
                })
            })
    }, [token])

    function handleResendEmailChange(
        event: ChangeEvent<HTMLInputElement>,
    ): void {
        setResendEmailInput(event.target.value)
        setResendEmailError(undefined)
        setResendFormError(undefined)
        setResendSuccessMessage(undefined)
    }

    async function handleResendSubmit(
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        if (isResending) {
            return
        }

        const validationError =
            validateResendEmail(resendEmailInput)

        if (validationError) {
            setResendEmailError(validationError)
            return
        }

        setIsResending(true)
        setResendEmailError(undefined)
        setResendFormError(undefined)
        setResendSuccessMessage(undefined)

        try {
            await resendVerificationEmail(
                resendEmailInput.trim(),
            )

            setResendSuccessMessage(
                'Falls ein Benutzerkonto mit dieser E-Mail-Adresse existiert, wurde eine neue Bestätigungs-E-Mail versendet.',
            )

            setResendEmailInput('')
        } catch (error) {
            if (error instanceof ApiRequestError) {
                setResendFormError(
                    mapResendError(
                        error.status,
                        error.code,
                        error.message,
                    ),
                )
            } else {
                setResendFormError(
                    'Die Bestätigungs-E-Mail konnte nicht erneut gesendet werden. Bitte versuche es erneut.',
                )
            }
        } finally {
            setIsResending(false)
        }
    }

    return (
        <div className={styles.page}>
            <section
                className={styles.card}
                aria-labelledby="verification-heading"
            >
                <p className={styles.eyebrow}>
                    E-Mail-Verifikation
                </p>

                {state.status === 'verifying' && (
                    <div className={styles.loaderWrapper}>
                        <Loader />

                        <h1 id="verification-heading">
                            E-Mail-Adresse wird bestätigt …
                        </h1>

                        <p
                            className={styles.description}
                            role="status"
                        >
                            Bitte habe einen kurzen Moment
                            Geduld.
                        </p>
                    </div>
                )}

                {state.status === 'success' && (
                    <div>
                        <header className={styles.header}>
                            <span
                                className={
                                    styles.successHeaderIcon
                                }
                                aria-hidden="true"
                            >
                                ✓
                            </span>

                            <h1 id="verification-heading">
                                E-Mail-Adresse bestätigt
                            </h1>
                        </header>

                        <div
                            className={
                                styles.successMessage
                            }
                            role="status"
                        >
                            <p>
                                Deine E-Mail-Adresse wurde
                                erfolgreich bestätigt. Dein
                                Konto ist jetzt vollständig
                                aktiviert und du kannst dich
                                anmelden.
                            </p>
                        </div>

                        <Link
                            to="/login"
                            className={styles.button}
                        >
                            Zum Login
                        </Link>
                    </div>
                )}

                {(state.status === 'error' ||
                    state.status === 'no_token') && (
                    <div>
                        <header className={styles.header}>
                            <span
                                className={
                                    styles.errorHeaderIcon
                                }
                                aria-hidden="true"
                            >
                                !
                            </span>

                            <h1 id="verification-heading">
                                {state.status === 'error'
                                    ? 'Bestätigung fehlgeschlagen'
                                    : 'Bestätigungslink unvollständig'}
                            </h1>
                        </header>

                        <div
                            className={styles.formError}
                            role="alert"
                        >
                            <span
                                className={styles.errorIcon}
                                aria-hidden="true"
                            >
                                !
                            </span>

                            <span>
                                {state.status === 'error'
                                    ? state.message
                                    : 'Es wurde kein Bestätigungstoken übergeben. Bitte verwende den vollständigen Link aus deiner E-Mail.'}
                            </span>
                        </div>

                        <section
                            className={
                                styles.resendSection
                            }
                            aria-labelledby="resend-heading"
                        >
                            <h2 id="resend-heading">
                                Bestätigungs-E-Mail erneut
                                anfordern
                            </h2>

                            <p
                                className={
                                    styles.description
                                }
                            >
                                Gib deine E-Mail-Adresse
                                ein, um einen neuen
                                Bestätigungslink zu
                                erhalten.
                            </p>

                            <form
                                className={styles.form}
                                onSubmit={
                                    handleResendSubmit
                                }
                                noValidate
                                aria-busy={isResending}
                            >
                                <div
                                    className={
                                        styles.field
                                    }
                                >
                                    <label htmlFor="resend-email">
                                        E-Mail-Adresse
                                    </label>

                                    <input
                                        id="resend-email"
                                        name="email"
                                        type="email"
                                        inputMode="email"
                                        autoComplete="email"
                                        autoCapitalize="none"
                                        spellCheck={false}
                                        value={
                                            resendEmailInput
                                        }
                                        onChange={
                                            handleResendEmailChange
                                        }
                                        aria-invalid={
                                            resendEmailError !==
                                            undefined
                                        }
                                        aria-describedby={
                                            resendEmailError
                                                ? 'resend-email-error'
                                                : undefined
                                        }
                                    />

                                    {resendEmailError && (
                                        <p
                                            id="resend-email-error"
                                            className={
                                                styles.fieldError
                                            }
                                            role="alert"
                                        >
                                            {
                                                resendEmailError
                                            }
                                        </p>
                                    )}
                                </div>

                                {resendFormError && (
                                    <div
                                        className={
                                            styles.formError
                                        }
                                        role="alert"
                                    >
                                        <span
                                            className={
                                                styles.errorIcon
                                            }
                                            aria-hidden="true"
                                        >
                                            !
                                        </span>

                                        <span>
                                            {
                                                resendFormError
                                            }
                                        </span>
                                    </div>
                                )}

                                {resendSuccessMessage && (
                                    <div
                                        className={
                                            styles.successMessage
                                        }
                                        role="status"
                                    >
                                        <span
                                            className={
                                                styles.successIcon
                                            }
                                            aria-hidden="true"
                                        >
                                            ✓
                                        </span>

                                        <span>
                                            {
                                                resendSuccessMessage
                                            }
                                        </span>
                                    </div>
                                )}

                                <button
                                    type="submit"
                                    className={
                                        styles.button
                                    }
                                    disabled={isResending}
                                    aria-busy={
                                        isResending
                                    }
                                >
                                    {isResending
                                        ? 'Wird versendet …'
                                        : 'Neue Bestätigungs-E-Mail senden'}
                                </button>
                            </form>
                        </section>

                        <div
                            className={
                                styles.footerLinks
                            }
                        >
                            <Link
                                to="/login"
                                className={
                                    styles.loginLink
                                }
                            >
                                Zurück zum Login
                            </Link>
                        </div>
                    </div>
                )}
            </section>
        </div>
    )
}