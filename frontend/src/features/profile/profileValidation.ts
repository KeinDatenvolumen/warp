// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {PASSWORD_MIN_LENGTH} from '../password-reset/passwordResetValidation'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export interface ChangeEmailValues {
    newEmail: string
    password: string
}

export interface ChangeEmailFieldErrors {
    newEmail?: string
    password?: string
}

export const CHANGE_EMAIL_FIELD_ORDER: Array<keyof ChangeEmailFieldErrors> = [
    'newEmail',
    'password'
]

export interface ChangePasswordValues {
    currentPassword: string
    newPassword: string
    passwordConfirmation: string
}

export interface ChangePasswordFieldErrors {
    currentPassword?: string
    newPassword?: string
    passwordConfirmation?: string
}

export const CHANGE_PASSWORD_FIELD_ORDER:
    Array<keyof ChangePasswordFieldErrors> = [
    'currentPassword',
    'newPassword',
    'passwordConfirmation'
]

export function validateChangeEmail(
    values: ChangeEmailValues,
    currentEmail: string
): ChangeEmailFieldErrors {
    const errors: ChangeEmailFieldErrors = {}
    const newEmail = values.newEmail.trim()

    if (!newEmail) {
        errors.newEmail = 'Bitte gib die neue E-Mail-Adresse ein.'
    } else if (!EMAIL_PATTERN.test(newEmail)) {
        errors.newEmail = 'Bitte gib eine gültige E-Mail-Adresse ein.'
    } else if (newEmail.toLowerCase() === currentEmail.toLowerCase()) {
        errors.newEmail =
            'Die neue E-Mail-Adresse muss sich von der aktuellen unterscheiden.'
    }

    if (!values.password) {
        errors.password = 'Bitte gib dein Passwort zur Bestätigung ein.'
    }

    return errors
}

export function validateChangePassword(
    values: ChangePasswordValues
): ChangePasswordFieldErrors {
    const errors: ChangePasswordFieldErrors = {}

    if (!values.currentPassword) {
        errors.currentPassword = 'Bitte gib dein aktuelles Passwort ein.'
    }

    if (!values.newPassword) {
        errors.newPassword = 'Bitte gib ein neues Passwort ein.'
    } else if (values.newPassword.length < PASSWORD_MIN_LENGTH) {
        errors.newPassword =
            'Das Passwort muss mindestens zwölf Zeichen lang sein.'
    } else if (values.newPassword === values.currentPassword) {
        errors.newPassword =
            'Das neue Passwort muss sich vom aktuellen Passwort unterscheiden.'
    }

    if (!values.passwordConfirmation) {
        errors.passwordConfirmation = 'Bitte wiederhole das neue Passwort.'
    } else if (values.newPassword !== values.passwordConfirmation) {
        errors.passwordConfirmation =
            'Die eingegebenen Passwörter stimmen nicht überein.'
    }

    return errors
}