// [KI-Log Nr. KI-188] (c4f557c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-013): route calculation, departure time selection, and result display
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export function formatDistance(
    distanceMeters: number,
): string {
    if (
        !Number.isFinite(distanceMeters) ||
        distanceMeters < 0
    ) {
        return 'Nicht verfügbar'
    }

    if (distanceMeters < 1000) {
        return `${Math.round(distanceMeters)} m`
    }

    return `${(distanceMeters / 1000).toLocaleString(
        'de-DE',
        {
            minimumFractionDigits: 1,
            maximumFractionDigits: 1,
        },
    )} km`
}

export function formatDuration(
    durationSeconds: number,
): string {
    if (
        !Number.isFinite(durationSeconds) ||
        durationSeconds < 0
    ) {
        return 'Nicht verfügbar'
    }

    const totalMinutes = Math.round(
        durationSeconds / 60,
    )
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60

    if (hours === 0) {
        return `${minutes} Min.`
    }

    if (minutes === 0) {
        return `${hours} Std.`
    }

    return `${hours} Std. ${minutes} Min.`
}

export function formatArrivalTime(
    arrivalTime: string,
): string {
    const date = new Date(arrivalTime)

    if (Number.isNaN(date.getTime())) {
        return 'Nicht verfügbar'
    }

    return new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}