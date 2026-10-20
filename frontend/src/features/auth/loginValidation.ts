export interface LoginValues {
    email: string
    password: string
}

export interface LoginFieldErrors {
    email?: string
    password?: string
}

export const LOGIN_FIELD_ORDER = ['email', 'password'] as const

export function validateLogin(
    values: LoginValues
): LoginFieldErrors {
    const errors: LoginFieldErrors = {}
    const email = values.email.trim()

    if (!email) {
        errors.email = 'Bitte gib deine E-Mail-Adresse ein.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errors.email = 'Bitte gib eine gültige E-Mail-Adresse ein.'
    }

    if (values.password.length === 0) {
        errors.password = 'Bitte gib dein Passwort ein.'
    }

    return errors
}