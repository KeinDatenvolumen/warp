// [KI-Log Nr. KI-15] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Routenplaner-UI & Polling (WARP-013)
import {
    describe,
    expect,
    it,
} from 'vitest'
import {
    createMaximumDepartureTime,
    formatDateTimeLocal,
    isDepartureTimeInFuture,
    isDepartureTimeWithinForecastPeriod,
    roundUpToNextDepartureTime,
    toDepartureTimeIso,
} from './departureTime'

describe('departureTime', () => {
    it('rounds up to the next quarter hour', () => {
        const result = roundUpToNextDepartureTime(
            new Date(2026, 9, 24, 13, 26, 54),
        )

        expect(formatDateTimeLocal(result)).toBe(
            '2026-10-24T13:30',
        )
    })

    it('uses the next quarter hour for an exact value', () => {
        const result = roundUpToNextDepartureTime(
            new Date(2026, 9, 24, 13, 30, 0),
        )

        expect(formatDateTimeLocal(result)).toBe(
            '2026-10-24T13:45',
        )
    })

    it('accepts a departure time in the future', () => {
        expect(
            isDepartureTimeInFuture(
                '2026-10-24T14:00',
                new Date(2026, 9, 24, 13, 30),
            ),
        ).toBe(true)
    })

    it('rejects a departure time in the past', () => {
        expect(
            isDepartureTimeInFuture(
                '2026-10-24T13:00',
                new Date(2026, 9, 24, 13, 30),
            ),
        ).toBe(false)
    })

    it('converts the local input into ISO 8601', () => {
        const result = toDepartureTimeIso(
            '2026-10-24T14:00',
        )

        expect(new Date(result).toISOString()).toBe(result)
    })

    it('validates departure time within forecast horizon of 7 days', () => {
        const now = new Date(2026, 9, 24, 12, 0)
        // 3 days in future -> valid
        expect(
            isDepartureTimeWithinForecastPeriod('2026-10-27T12:00', now),
        ).toBe(true)

        // 7 days in future -> valid
        expect(
            isDepartureTimeWithinForecastPeriod('2026-10-31T12:00', now),
        ).toBe(true)

        // 8 days in future -> invalid (outside forecast horizon)
        expect(
            isDepartureTimeWithinForecastPeriod('2026-11-02T12:00', now),
        ).toBe(false)

        // In the past -> invalid
        expect(
            isDepartureTimeWithinForecastPeriod('2026-10-23T12:00', now),
        ).toBe(false)
    })

    it('generates maximum departure time 7 days ahead', () => {
        const now = new Date(2026, 9, 24, 12, 0)
        expect(createMaximumDepartureTime(now)).toBe('2026-10-31T12:00')
    })
})
