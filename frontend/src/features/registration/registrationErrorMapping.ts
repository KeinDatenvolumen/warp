import type {ApiFieldError} from '../../types/apiError'
import type {
    RegistrationFieldErrors
} from './registrationValidation'

export interface RegistrationErrorState {
    fieldErrors: RegistrationFieldErrors
    formError?: string
}

export function mapRegistrationApiErrors(
    status: number | undefined,
    code: string | undefined,
    fieldErrors: ApiFieldError[],
    detail: string
): RegistrationErrorState {
    const mapped: RegistrationErrorState = {
        fieldErrors: {}
    }

    if (
        status === 409 &&
        code === 'EMAIL_ALREADY_EXISTS'
    ) {
        mapped.fieldErrors.email =
            'Für diese E-Mail-Adresse ist bereits ein Benutzerkonto vorhanden.'
        return mapped
    }

    if (
        status === 422 &&
        code === 'INVALID_PASSWORD'
    ) {
        mapped.fieldErrors.password =
            'Das Passwort erfüllt die Anforderungen nicht.'
        return mapped
    }

    let containsUnknownFieldError = false

    for (const fieldError of fieldErrors) {
        const message = fieldError.message

        if (!message) {
            containsUnknownFieldError = true
            continue
        }

        if (
            fieldError.field === 'email' ||
            fieldError.code === 'EMAIL_ALREADY_EXISTS'
        ) {
            mapped.fieldErrors.email = message
            continue
        }

        if (
            fieldError.field === 'password' ||
            fieldError.code === 'INVALID_PASSWORD'
        ) {
            mapped.fieldErrors.password = message
            continue
        }

        if (fieldError.field === 'name') {
            mapped.fieldErrors.name = message
            continue
        }

        containsUnknownFieldError = true
    }

    if (
        Object.keys(mapped.fieldErrors).length === 0 ||
        containsUnknownFieldError
    ) {
        mapped.formError =
            detail ||
            'Die Registrierung konnte nicht abgeschlossen werden. Bitte versuche es erneut.'
    }

    return mapped
}