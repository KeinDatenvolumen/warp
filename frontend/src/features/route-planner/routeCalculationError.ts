import {
    containsSessionInvalidCode,
    extractGraphQlErrorCodes,
    extractGraphQlErrorMessage,
    isNetworkError,
} from '../../api/graphqlErrors'

export interface RouteCalculationAction {
    label: string
    to?: string
    href?: string
    onClick?: () => void
}

export interface RouteCalculationErrorDetail {
    title: string
    message: string
    code?: string
    action?: RouteCalculationAction
    secondaryAction?: RouteCalculationAction
}

export function parseRouteCalculationError(
    error: unknown
): RouteCalculationErrorDetail {
    if (containsSessionInvalidCode(error)) {
        return {
            title: 'Anmeldung erforderlich',
            message:
                'Für die Routenberechnung ist eine Anmeldung erforderlich. Bitte melde dich an oder registriere dich, um deine wetteroptimierte Route zu berechnen.',
            code: 'SESSION_INVALID',
            action: {
                label: 'Jetzt anmelden',
                to: '/login',
            },
            secondaryAction: {
                label: 'Konto erstellen',
                to: '/register',
            },
        }
    }

    const codes = extractGraphQlErrorCodes(error)

    if (codes.includes('EMAIL_NOT_VERIFIED')) {
        return {
            title: 'E-Mail-Bestätigung erforderlich',
            message:
                'Deine E-Mail-Adresse ist noch nicht bestätigt. Bitte verifiziere deine E-Mail-Adresse, um Routen zu berechnen.',
            code: 'EMAIL_NOT_VERIFIED',
            action: {
                label: 'E-Mail bestätigen',
                to: '/verify-email',
            },
        }
    }

    if (
        codes.includes('INVALID_ROUTE_REQUEST') ||
        codes.includes('BAD_REQUEST') ||
        codes.includes('VALIDATION_FAILED')
    ) {
        return {
            title: 'Ungültige Routenanfrage',
            message:
                'Die Routenanfrage konnte nicht verarbeitet werden. Bitte prüfe Start- und Zielort sowie die gewählte Abfahrtszeit.',
            code: 'INVALID_ROUTE_REQUEST',
        }
    }

    if (codes.includes('ROUTE_NOT_FOUND')) {
        return {
            title: 'Keine Route gefunden',
            message:
                'Für die ausgewählten Orte und das gewählte Profil konnte keine passende Route berechnet werden. Bitte wähle andere Orte oder ein anderes Profil.',
            code: 'ROUTE_NOT_FOUND',
        }
    }

    if (
        codes.includes('ROUTING_SERVICE_UNAVAILABLE') ||
        codes.includes('DOWNSTREAM_SERVICE_UNAVAILABLE') ||
        codes.includes('DOWNSTREAM_SERVICE_ERROR')
    ) {
        return {
            title: 'Dienst vorübergehend nicht verfügbar',
            message:
                'Der Routenberechnungsdienst oder Wetterdienst ist momentan nicht erreichbar. Bitte versuche es in wenigen Minuten erneut.',
            code: 'ROUTING_SERVICE_UNAVAILABLE',
        }
    }

    if (codes.includes('GEOCODING_SERVICE_UNAVAILABLE')) {
        return {
            title: 'Ortssuche nicht verfügbar',
            message:
                'Die Ortssuche ist vorübergehend nicht erreichbar. Bitte versuche es später noch einmal.',
            code: 'GEOCODING_SERVICE_UNAVAILABLE',
        }
    }

    if (isNetworkError(error)) {
        return {
            title: 'Verbindungsfehler',
            message:
                'Es konnte keine Verbindung zum Server aufgebaut werden. Bitte prüfe deine Internetverbindung und versuche es erneut.',
            code: 'NETWORK_ERROR',
        }
    }

    const serverMessage = extractGraphQlErrorMessage(error)
    if (serverMessage) {
        return {
            title: 'Fehler bei der Routenberechnung',
            message: serverMessage,
        }
    }

    return {
        title: 'Fehler bei der Routenberechnung',
        message:
            'Die Routenberechnung konnte nicht gestartet werden. Bitte versuche es später noch einmal.',
    }
}

export function parseCalculationStatusError(
    calculation: {
        status?: string | null
        error?: { code?: string | null; message?: string | null } | null
    } | null | undefined
): RouteCalculationErrorDetail | null {
    if (calculation?.status !== 'FAILED') {
        return null
    }

    const code = calculation.error?.code
    const rawMessage = calculation.error?.message

    if (
        code === 'ROUTE_NOT_FOUND' ||
        rawMessage?.toLowerCase().includes('keine route')
    ) {
        return {
            title: 'Keine Route gefunden',
            message:
                rawMessage ??
                'Für die gewählten Koordinaten konnte keine passende Route berechnet werden. Bitte wähle andere Orte oder ein anderes Profil.',
            code: 'ROUTE_NOT_FOUND',
        }
    }

    if (
        code === 'ROUTING_SERVICE_UNAVAILABLE' ||
        rawMessage?.toLowerCase().includes('nicht verfügbar')
    ) {
        return {
            title: 'Routendienst nicht verfügbar',
            message:
                rawMessage ??
                'Der Routendienst ist vorübergehend überlastet oder nicht erreichbar. Bitte versuche es in Kürze erneut.',
            code: 'ROUTING_SERVICE_UNAVAILABLE',
        }
    }

    return {
        title: 'Routenberechnung fehlgeschlagen',
        message:
            rawMessage ??
            'Für diese Anfrage konnte keine wetteroptimierte Route berechnet werden.',
        code: code ?? 'CALCULATION_FAILED',
    }
}
