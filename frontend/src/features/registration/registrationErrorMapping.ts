// [KI-Log Nr. KI-087] (486dab2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-090] (52b5c38) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: registration error handling and design (WARP-005)
// [KI-Log Nr. KI-109] (de61707) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-111] (2b00488) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: registration error handling and design (WARP-005)
// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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