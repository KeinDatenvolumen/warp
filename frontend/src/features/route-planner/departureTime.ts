// [KI-Log Nr. KI-15] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Routenplaner-UI & Polling (WARP-013)
const DEPARTURE_TIME_STEP_MINUTES = 15
export const FORECAST_HORIZON_DAYS = 7

function pad(value: number): string {
    return value.toString().padStart(2, '0')
}

export function roundUpToNextDepartureTime(
    date: Date,
): Date {
    const roundedDate = new Date(date)
    const minutes = roundedDate.getMinutes()
    const remainder = minutes % DEPARTURE_TIME_STEP_MINUTES
    const minutesToAdd =
        remainder === 0 && roundedDate.getSeconds() === 0
            ? DEPARTURE_TIME_STEP_MINUTES
            : DEPARTURE_TIME_STEP_MINUTES - remainder

    roundedDate.setMinutes(
        minutes + minutesToAdd,
        0,
        0,
    )

    return roundedDate
}

export function formatDateTimeLocal(date: Date): string {
    return [
        date.getFullYear(),
        '-',
        pad(date.getMonth() + 1),
        '-',
        pad(date.getDate()),
        'T',
        pad(date.getHours()),
        ':',
        pad(date.getMinutes()),
    ].join('')
}

export function createInitialDepartureTime(): string {
    return formatDateTimeLocal(
        roundUpToNextDepartureTime(new Date()),
    )
}

export function createMinimumDepartureTime(): string {
    return formatDateTimeLocal(
        roundUpToNextDepartureTime(new Date()),
    )
}

export function createMaximumDepartureTime(now = new Date()): string {
    const maxDate = new Date(now)
    maxDate.setDate(maxDate.getDate() + FORECAST_HORIZON_DAYS)
    return formatDateTimeLocal(maxDate)
}

export function isDepartureTimeInFuture(
    value: string,
    now = new Date(),
): boolean {
    if (!value) {
        return false
    }

    const departureTime = new Date(value)

    return (
        !Number.isNaN(departureTime.getTime()) &&
        departureTime.getTime() > now.getTime()
    )
}

export function isDepartureTimeWithinForecastPeriod(
    value: string,
    now = new Date(),
): boolean {
    if (!isDepartureTimeInFuture(value, now)) {
        return false
    }

    const departureTime = new Date(value)
    const maxDate = new Date(now)
    maxDate.setDate(maxDate.getDate() + FORECAST_HORIZON_DAYS)

    return departureTime.getTime() <= maxDate.getTime()
}

export function toDepartureTimeIso(
    value: string,
): string {
    const departureTime = new Date(value)

    if (Number.isNaN(departureTime.getTime())) {
        throw new Error('Invalid departure time')
    }

    return departureTime.toISOString()
}
