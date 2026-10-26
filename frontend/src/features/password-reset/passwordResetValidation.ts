export const PASSWORD_MIN_LENGTH = 12

export interface ForgotPasswordValues {
    email: string
}

export interface ForgotPasswordFieldErrors {
    email?: string
}

export interface ResetPasswordValues {
    newPassword: string
    passwordConfirmation: string
}

export interface ResetPasswordFieldErrors {
    newPassword?: string
    passwordConfirmation?: string
}

export const RESET_PASSWORD_FIELD_ORDER:
    Array<keyof ResetPasswordFieldErrors> = [
    'newPassword',
    'passwordConfirmation'
]

export function validateForgotPassword(
    values: ForgotPasswordValues
): ForgotPasswordFieldErrors {
    const errors: ForgotPasswordFieldErrors = {}
    const normalizedEmail = values.email.trim()

    if (!normalizedEmail) {
        errors.email =
            'Bitte gib deine E-Mail-Adresse ein.'
    } else if (
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
            normalizedEmail
        )
    ) {
        errors.email =
            'Bitte gib eine gültige E-Mail-Adresse ein.'
    }

    return errors
}

export function validateResetPassword(
    values: ResetPasswordValues
): ResetPasswordFieldErrors {
    const errors: ResetPasswordFieldErrors = {}

    if (!values.newPassword) {
        errors.newPassword =
            'Bitte gib ein neues Passwort ein.'
    } else if (
        values.newPassword.length < PASSWORD_MIN_LENGTH
    ) {
        errors.newPassword =
            'Das Passwort muss mindestens zwölf Zeichen lang sein.'
    }

    if (!values.passwordConfirmation) {
        errors.passwordConfirmation =
            'Bitte wiederhole das neue Passwort.'
    } else if (
        values.newPassword !== values.passwordConfirmation
    ) {
        errors.passwordConfirmation =
            'Die eingegebenen Passwörter stimmen nicht überein.'
    }

    return errors
}