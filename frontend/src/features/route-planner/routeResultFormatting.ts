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