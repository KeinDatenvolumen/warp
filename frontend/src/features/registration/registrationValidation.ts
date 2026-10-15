export interface RegistrationValues {
    name: string
    email: string
    password: string
}

export interface RegistrationFieldErrors {
    name?: string
    email?: string
    password?: string
}

export function validateRegistration(values: RegistrationValues): RegistrationFieldErrors {
    const errors: RegistrationFieldErrors = {}

    if (!values.name.trim()) {
        errors.name = 'Bitte gib deinen Namen ein.'
    }

    if (!values.email.trim()) {
        errors.email =
            'Bitte gib deine E-Mail-Adresse ein.'
    } else if (
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
            values.email
        )
    ) {
        errors.email =
            'Bitte gib eine gültige E-Mail-Adresse ein.'
    }

    if (!values.password) {
        errors.password =
            'Bitte gib ein Passwort ein.'
    } else if (values.password.length < 12) {
        errors.password =
            'Das Passwort muss mindestens zwölf Zeichen lang sein.'
    }

    return errors
}