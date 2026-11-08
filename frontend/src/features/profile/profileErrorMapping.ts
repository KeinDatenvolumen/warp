// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import type {ApiRequestError} from '../../api/restClient'
import type {
    ChangeEmailFieldErrors,
    ChangePasswordFieldErrors
} from './profileValidation'

const SESSION_EXPIRED_MESSAGE =
    'Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.'

const SERVICE_UNAVAILABLE_MESSAGE =
    'Der Dienst ist momentan nicht erreichbar. Bitte versuche es später erneut.'

export interface ChangeEmailErrorResult {
    fieldErrors: ChangeEmailFieldErrors
    formError?: string
}

export interface ChangePasswordErrorResult {
    fieldErrors: ChangePasswordFieldErrors
    formError?: string
}

export function mapChangeEmailError(
    error: ApiRequestError
): ChangeEmailErrorResult {
    if (error.status === 401) {
        return {fieldErrors: {}, formError: SESSION_EXPIRED_MESSAGE}
    }

    if (error.code === 'INVALID_PASSWORD') {
        return {fieldErrors: {password: 'Das Passwort ist nicht korrekt.'}}
    }

    if (error.code === 'EMAIL_ALREADY_EXISTS') {
        return {
            fieldErrors: {
                newEmail:
                    'Für diese E-Mail-Adresse existiert bereits ein Benutzerkonto.'
            }
        }
    }

    if (error.code === 'INVALID_ACCOUNT_CHANGE_REQUEST') {
        const message = error.fieldErrors.find(
            (fieldError) => fieldError.field === 'newEmail'
        )?.message

        return {
            fieldErrors: {
                newEmail:
                    message ?? 'Bitte gib eine gültige E-Mail-Adresse ein.'
            }
        }
    }

    if (error.status === 503) {
        return {fieldErrors: {}, formError: SERVICE_UNAVAILABLE_MESSAGE}
    }

    return {
        fieldErrors: {},
        formError:
            'Die E-Mail-Adresse konnte nicht geändert werden. Bitte versuche es erneut.'
    }
}

export function mapChangePasswordError(
    error: ApiRequestError
): ChangePasswordErrorResult {
    if (error.status === 401) {
        return {fieldErrors: {}, formError: SESSION_EXPIRED_MESSAGE}
    }

    if (error.code === 'INVALID_PASSWORD') {
        return {
            fieldErrors: {
                currentPassword: 'Das aktuelle Passwort ist nicht korrekt.'
            }
        }
    }

    if (error.code === 'PASSWORD_REQUIREMENTS_NOT_MET') {
        return {
            fieldErrors: {
                newPassword:
                    'Das Passwort muss mindestens zwölf Zeichen lang sein.'
            }
        }
    }

    if (error.code === 'INVALID_ACCOUNT_CHANGE_REQUEST') {
        const message = error.fieldErrors.find(
            (fieldError) => fieldError.field === 'newPassword'
        )?.message

        return {
            fieldErrors: {
                newPassword:
                    message ?? 'Das neue Passwort ist ungültig.'
            }
        }
    }

    if (error.status === 503) {
        return {fieldErrors: {}, formError: SERVICE_UNAVAILABLE_MESSAGE}
    }

    return {
        fieldErrors: {},
        formError:
            'Das Passwort konnte nicht geändert werden. Bitte versuche es erneut.'
    }
}