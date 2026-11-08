// [KI-Log Nr. KI-167] (af1ca24) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: add frontend email verification (WARP-007)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
