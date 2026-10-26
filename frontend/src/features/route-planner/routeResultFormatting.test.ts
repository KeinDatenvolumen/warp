import {
    describe,
    expect,
    it,
} from 'vitest'
import {
    formatArrivalTime,
    formatDistance,
    formatDuration,
} from './routeResultFormatting'

describe('routeResultFormatting', () => {
    it('formats a distance in metres', () => {
        expect(formatDistance(850)).toBe('850 m')
    })

    it('formats a distance in kilometres', () => {
        expect(formatDistance(12500)).toBe('12,5 km')
    })

    it('formats a duration below one hour', () => {
        expect(formatDuration(1800)).toBe('30 Min.')
    })

    it('formats hours and minutes', () => {
        expect(formatDuration(5400)).toBe(
            '1 Std. 30 Min.',
        )
    })

    it('formats a valid arrival time', () => {
        expect(
            formatArrivalTime(
                '2026-10-24T14:30:00Z',
            ),
        ).not.toBe('Nicht verfügbar')
    })

    it('rejects invalid result values', () => {
        expect(formatDistance(-1)).toBe(
            'Nicht verfügbar',
        )
        expect(formatDuration(Number.NaN)).toBe(
            'Nicht verfügbar',
        )
        expect(formatArrivalTime('invalid')).toBe(
            'Nicht verfügbar',
        )
    })
})