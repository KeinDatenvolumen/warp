// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
const dateFormatter = new Intl.DateTimeFormat('de-DE', {dateStyle: 'medium'})

const dateTimeFormatter = new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
    timeStyle: 'short'
})

function isValid(date: Date): boolean {
    return !Number.isNaN(date.getTime())
}

export function formatDate(iso: string): string {
    const date = new Date(iso)
    return isValid(date) ? dateFormatter.format(date) : '–'
}

export function formatDateTime(iso: string): string {
    const date = new Date(iso)
    return isValid(date) ? dateTimeFormatter.format(date) : '–'
}

export function formatDistance(meters: number): string {
    if (meters < 1000) {
        return `${Math.round(meters)} m`
    }

    return `${(meters / 1000).toLocaleString('de-DE', {
        maximumFractionDigits: 1
    })} km`
}

export function formatDuration(seconds: number): string {
    const totalMinutes = Math.max(0, Math.round(seconds / 60))
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60

    return hours === 0 ? `${minutes} Min.` : `${hours} Std. ${minutes} Min.`
}