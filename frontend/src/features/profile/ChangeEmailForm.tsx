import {
    type ChangeEvent,
    type FormEvent,
    useRef,
    useState
} from 'react'
import {changeEmail} from '../../api/accountApi'
import {ApiRequestError} from '../../api/restClient'
import {mapChangeEmailError} from './profileErrorMapping'
import {
    CHANGE_EMAIL_FIELD_ORDER,
    validateChangeEmail,
    type ChangeEmailFieldErrors,
    type ChangeEmailValues
} from './profileValidation'
import styles from './Profile.module.css'

const GENERIC_ERROR =
    'Die E-Mail-Adresse konnte nicht geändert werden. Bitte versuche es erneut.'

const initialValues: ChangeEmailValues = {newEmail: '', password: ''}

interface ChangeEmailFormProps {
    currentEmail: string
    onChanged: () => void
}

export default function ChangeEmailForm({
                                            currentEmail,
                                            onChanged
                                        }: ChangeEmailFormProps) {
    const [values, setValues] = useState<ChangeEmailValues>(initialValues)
    const [errors, setErrors] = useState<ChangeEmailFieldErrors>({})
    const [formError, setFormError] = useState<string>()
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [isSuccessful, setIsSuccessful] = useState(false)
    const isSubmittingRef = useRef(false)
    const newEmailRef = useRef<HTMLInputElement>(null)
    const passwordRef = useRef<HTMLInputElement>(null)

    function handleChange(event: ChangeEvent<HTMLInputElement>): void {
        const {name, value} = event.target

        setValues((current) => ({...current, [name]: value}))
        setErrors((current) => ({...current, [name]: undefined}))
        setFormError(undefined)
        setIsSuccessful(false)
    }

    function focusFirstInvalidField(
        validationErrors: ChangeEmailFieldErrors
    ): void {
        const firstInvalid = CHANGE_EMAIL_FIELD_ORDER.find(
            (field) => validationErrors[field]
        )

        if (firstInvalid === 'newEmail') {
            newEmailRef.current?.focus()
        } else if (firstInvalid === 'password') {
            passwordRef.current?.focus()
        }
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        if (isSubmittingRef.current) {
            return
        }

        const validationErrors = validateChangeEmail(values, currentEmail)

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
            await changeEmail({
                newEmail: values.newEmail.trim(),
                password: values.password
            })

            setValues(initialValues)
            setIsSuccessful(true)
            onChanged()
        } catch (error) {
            if (error instanceof ApiRequestError) {
                const mapped = mapChangeEmailError(error)

                setErrors(mapped.fieldErrors)
                setFormError(mapped.formError)
            } else {
                setFormError(GENERIC_ERROR)
            }

            setValues((current) => ({...current, password: ''}))
        } finally {
            isSubmittingRef.current = false
            setIsSubmitting(false)
        }
    }

    return (
        <section className={styles.section} aria-labelledby="change-email-title">
            <h2 id="change-email-title">E-Mail-Adresse ändern</h2>
            <p className={styles.sectionDescription}>
                Nach der Änderung musst du die neue Adresse über einen Link in
                der Bestätigungs-E-Mail bestätigen. Bis dahin kannst du keine
                Routen berechnen oder speichern.
            </p>

            <form
                onSubmit={(event) => void handleSubmit(event)}
                noValidate
                aria-busy={isSubmitting}
            >
                <div className={styles.field}>
                    <label htmlFor="change-email-new">
                        Neue E-Mail-Adresse
                    </label>
                    <input
                        ref={newEmailRef}
                        id="change-email-new"
                        name="newEmail"
                        type="email"
                        autoComplete="email"
                        value={values.newEmail}
                        onChange={handleChange}
                        aria-invalid={Boolean(errors.newEmail)}
                        aria-describedby={
                            errors.newEmail ? 'change-email-new-error' : undefined
                        }
                    />
                    {errors.newEmail && (
                        <p
                            id="change-email-new-error"
                            className={styles.fieldError}
                            role="alert"
                        >
                            {errors.newEmail}
                        </p>
                    )}
                </div>

                <div className={styles.field}>
                    <label htmlFor="change-email-password">
                        Passwort zur Bestätigung
                    </label>
                    <input
                        ref={passwordRef}
                        id="change-email-password"
                        name="password"
                        type="password"
                        autoComplete="current-password"
                        value={values.password}
                        onChange={handleChange}
                        aria-invalid={Boolean(errors.password)}
                        aria-describedby={
                            errors.password
                                ? 'change-email-password-error'
                                : undefined
                        }
                    />
                    {errors.password && (
                        <p
                            id="change-email-password-error"
                            className={styles.fieldError}
                            role="alert"
                        >
                            {errors.password}
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
                        Deine E-Mail-Adresse wurde geändert. Wir haben dir eine
                        Bestätigungs-E-Mail an die neue Adresse geschickt.
                    </p>
                )}

                <button
                    type="submit"
                    className={styles.button}
                    disabled={isSubmitting}
                    aria-busy={isSubmitting}
                >
                    {isSubmitting ? 'Wird gespeichert …' : 'E-Mail-Adresse ändern'}
                </button>
            </form>
        </section>
    )
}