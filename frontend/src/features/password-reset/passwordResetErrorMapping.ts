export function mapPasswordResetSendError(
    status: number,
    code?: string,
    defaultMessage?: string
): string {
    if (status === 429) {
        return 'Zu viele Anfragen. Bitte versuche es später erneut.'
    }

    return (
        defaultMessage ||
        'Die Passwort-Rücksetzungs-E-Mail konnte nicht versendet werden. Bitte versuche es später erneut.'
    )
}

export function mapPasswordResetConfirmError(
    status: number,
    code?: string,
    defaultMessage?: string
): string {
    if (
        code === 'INVALID_RESET_TOKEN' ||
        status === 400
    ) {
        return 'Der Passwort-Rücksetzungslink ist ungültig, abgelaufen oder wurde bereits verwendet.'
    }

    if (
        code === 'PASSWORD_REQUIREMENTS_NOT_MET' ||
        status === 422
    ) {
        return 'Das Passwort muss mindestens zwölf Zeichen lang sein.'
    }

    if (status === 429) {
        return 'Zu viele Anfragen. Bitte versuche es später erneut.'
    }

    return (
        defaultMessage ||
        'Das Passwort konnte nicht zurückgesetzt werden. Bitte versuche es erneut.'
    )
}