export function mapConfirmError(
    status: number,
    code?: string,
    defaultMessage?: string
): string {
    if (code === 'INVALID_VERIFICATION_TOKEN' || status === 400) {
        return 'Der Bestätigungslink ist ungültig, abgelaufen oder wurde bereits verwendet.'
    }

    if (status === 429) {
        return 'Zu viele Anfragen. Bitte versuche es später erneut.'
    }

    return (
        defaultMessage ||
        'Die E-Mail-Adresse konnte nicht bestätigt werden. Bitte versuche es erneut.'
    )
}

export function mapResendError(
    status: number,
    code?: string,
    defaultMessage?: string
): string {
    if (status === 429) {
        return 'Zu viele Anfragen. Bitte versuche es später erneut.'
    }

    return (
        defaultMessage ||
        'Die Bestätigungs-E-Mail konnte nicht erneut gesendet werden. Bitte versuche es später erneut.'
    )
}
