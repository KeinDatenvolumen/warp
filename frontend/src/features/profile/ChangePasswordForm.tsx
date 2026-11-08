// [KI-Log Nr. KI-205] (13504a4) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: build profile page with account data, email and password change (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    type ChangeEvent,
    type FormEvent,
    useRef,
    useState
} from 'react'
import {changePassword} from '../../api/accountApi'
import {ApiRequestError} from '../../api/restClient'
import {mapChangePasswordError} from './profileErrorMapping'
import {
    CHANGE_PASSWORD_FIELD_ORDER,
    validateChangePassword,
    type ChangePasswordFieldErrors,
    type ChangePasswordValues
} from './profileValidation'
import styles from './Profile.module.css'

const GENERIC_ERROR =
    'Das Passwort konnte nicht geändert werden. Bitte versuche es erneut.'

const initialValues: ChangePasswordValues = {
    currentPassword: '',
    newPassword: '',
    passwordConfirmation: ''
}

export default function ChangePasswordForm() {
    const [values, setValues] =
        useState<ChangePasswordValues>(initialValues)
    const [errors, setErrors] = useState<ChangePasswordFieldErrors>({})
    const [formError, setFormError] = useState<string>()
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [isSuccessful, setIsSuccessful] = useState(false)
    const isSubmittingRef = useRef(false)
    const currentPasswordRef = useRef<HTMLInputElement>(null)
    const newPasswordRef = useRef<HTMLInputElement>(null)
    const confirmationRef = useRef<HTMLInputElement>(null)

    function handleChange(event: ChangeEvent<HTMLInputElement>): void {
        const {name, value} = event.target

        setValues((current) => ({...current, [name]: value}))
        setErrors((current) => ({...current, [name]: undefined}))
        setFormError(undefined)
        setIsSuccessful(false)
    }

    function focusFirstInvalidField(
        validationErrors: ChangePasswordFieldErrors
    ): void {
        const firstInvalid = CHANGE_PASSWORD_FIELD_ORDER.find(
            (field) => validationErrors[field]
        )

        if (firstInvalid === 'currentPassword') {
            currentPasswordRef.current?.focus()
        } else if (firstInvalid === 'newPassword') {
            newPasswordRef.current?.focus()
        } else if (firstInvalid === 'passwordConfirmation') {
            confirmationRef.current?.focus()
        }
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        if (isSubmittingRef.current) {
            return
        }

        const validationErrors = validateChangePassword(values)

        setErrors(validationErrors)
        setFormError(undefined)
        setIsSuccessful(false)

        if (Object.keys(validationErrors).length > 0) {
            focusFirstInvalidField(validationErrors)
            return
        }

        isSubmittingRef.current = true
        setIsSubmitting(true)

        try {
            await changePassword({
                currentPassword: values.currentPassword,
                newPassword: values.newPassword
            })

            setValues(initialValues)
            setIsSuccessful(true)
        } catch (error) {
            if (error instanceof ApiRequestError) {
                const mapped = mapChangePasswordError(error)

                setErrors(mapped.fieldErrors)
                setFormError(mapped.formError)
            } else {
                setFormError(GENERIC_ERROR)
            }

            setValues((current) => ({
                ...current,
                currentPassword: '',
                newPassword: '',
                passwordConfirmation: ''
            }))
        } finally {
            isSubmittingRef.current = false
            setIsSubmitting(false)
        }
    }

    return (
        <section
            className={styles.section}
            aria-labelledby="change-password-title"
        >
            <h2 id="change-password-title">Passwort ändern</h2>
            <p className={styles.sectionDescription}>
                Das neue Passwort muss mindestens zwölf Zeichen lang sein.
            </p>

            <form
                onSubmit={(event) => void handleSubmit(event)}
                noValidate
                aria-busy={isSubmitting}
            >
                <div className={styles.field}>
                    <label htmlFor="change-password-current">
                        Aktuelles Passwort
                    </label>
                    <input
                        ref={currentPasswordRef}
                        id="change-password-current"
                        name="currentPassword"
                        type="password"
                        autoComplete="current-password"
                        value={values.currentPassword}
                        onChange={handleChange}
                        aria-invalid={Boolean(errors.currentPassword)}
                        aria-describedby={
                            errors.currentPassword
                                ? 'change-password-current-error'
                                : undefined
                        }
                    />
                    {errors.currentPassword && (
                        <p
                            id="change-password-current-error"
                            className={styles.fieldError}
                            role="alert"
                        >
                            {errors.currentPassword}
                        </p>
                    )}
                </div>

                <div className={styles.field}>
                    <label htmlFor="change-password-new">Neues Passwort</label>
                    <input
                        ref={newPasswordRef}
                        id="change-password-new"
                        name="newPassword"
                        type="password"
                        autoComplete="new-password"
                        value={values.newPassword}
                        onChange={handleChange}
                        aria-invalid={Boolean(errors.newPassword)}
                        aria-describedby={
                            errors.newPassword
                                ? 'change-password-new-error'
                                : undefined
                        }
                    />
                    {errors.newPassword && (
                        <p
                            id="change-password-new-error"
                            className={styles.fieldError}
                            role="alert"
                        >
                            {errors.newPassword}
                        </p>
                    )}
                </div>

                <div className={styles.field}>
                    <label htmlFor="change-password-confirmation">
                        Neues Passwort wiederholen
                    </label>
                    <input
                        ref={confirmationRef}
                        id="change-password-confirmation"
                        name="passwordConfirmation"
                        type="password"
                        autoComplete="new-password"
                        value={values.passwordConfirmation}
                        onChange={handleChange}
                        aria-invalid={Boolean(errors.passwordConfirmation)}
                        aria-describedby={
                            errors.passwordConfirmation
                                ? 'change-password-confirmation-error'
                                : undefined
                        }
                    />
                    {errors.passwordConfirmation && (
                        <p
                            id="change-password-confirmation-error"
                            className={styles.fieldError}
                            role="alert"
                        >
                            {errors.passwordConfirmation}
                        </p>
                    )}
                </div>

                {formError && (
                    <p className={styles.formError} role="alert">
                        {formError}
                    </p>
                )}

                {isSuccessful && (
                    <p className={styles.successMessage} role="status">
                        Dein Passwort wurde geändert.
                    </p>
                )}

                <button
                    type="submit"
                    className={styles.button}
                    disabled={isSubmitting}
                    aria-busy={isSubmitting}
                >
                    {isSubmitting ? 'Wird gespeichert …' : 'Passwort ändern'}
                </button>
            </form>
        </section>
    )
}