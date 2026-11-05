// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
import {ApiRequestError} from '../../api/restClient'

export const INVALID_CREDENTIALS_MESSAGE =
    'Die E-Mail-Adresse oder das Passwort ist nicht korrekt.'

export const TECHNICAL_LOGIN_ERROR_MESSAGE =
    'Die Anmeldung konnte nicht abgeschlossen werden. Bitte versuche es erneut.'

const TOO_MANY_ATTEMPTS_MESSAGE =
    'Es wurden zu viele Anmeldeversuche durchgeführt.'

export interface LoginErrorState {
    message: string
    retryAfterSeconds?: number
}

export function formatWaitTime(seconds: number): string {
    if (seconds < 60) {
        return seconds === 1 ? '1 Sekunde' : `${seconds} Sekunden`
    }

    const minutes = Math.ceil(seconds / 60)

    return minutes === 1 ? '1 Minute' : `${minutes} Minuten`
}

export function mapLoginError(error: unknown): LoginErrorState {
    if (error instanceof ApiRequestError) {
        if (
            error.status === 401 ||
            error.code === 'INVALID_CREDENTIALS'
        ) {
            return {message: INVALID_CREDENTIALS_MESSAGE}
        }

        if (error.status === 429) {
            const retryAfterSeconds = error.retryAfterSeconds

            return {
                message: retryAfterSeconds
                    ? `${TOO_MANY_ATTEMPTS_MESSAGE} Bitte versuche es in ${formatWaitTime(retryAfterSeconds)} erneut.`
                    : `${TOO_MANY_ATTEMPTS_MESSAGE} Bitte versuche es später erneut.`,
                retryAfterSeconds
            }
        }
    }

    return {message: TECHNICAL_LOGIN_ERROR_MESSAGE}
}