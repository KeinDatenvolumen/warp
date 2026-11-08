// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import { describe, expect, it } from 'vitest'
import {
    findOptimalDepartureTimes,
    formatSlotTime,
} from './departureOptimizer'

describe('departureOptimizer', () => {
    it('formats ISO slot times to local HH:mm', () => {
        const formatted = formatSlotTime('2026-05-16T09:30:00Z')
        expect(formatted).toMatch(/\d{2}:\d{2}/)
    })

    it('computes diagonal sliding window trace across candidate slots in O(N*M)', () => {
        // 3 segments, 5 time slots
        const scoreMatrix = [
            [80, 85, 90, 70, 60], // Seg 0
            [75, 82, 88, 65, 55], // Seg 1
            [70, 80, 85, 60, 50], // Seg 2
        ]
        const reasonMatrix = [
            ['Klar', 'Klar', 'Klar', 'Wind', 'Regen'],
            ['Klar', 'Klar', 'Klar', 'Wind', 'Regen'],
            ['Klar', 'Klar', 'Klar', 'Wind', 'Regen'],
        ]
        const thunderMatrix = [
            [false, false, false, false, false],
            [false, false, false, false, false],
            [false, false, false, false, false],
        ]
        const segmentDurations = [15, 15, 15] // 15 min each
        const timeSlotIsos = [
            '2026-05-16T08:00:00Z',
            '2026-05-16T08:15:00Z',
            '2026-05-16T08:30:00Z',
            '2026-05-16T08:45:00Z',
            '2026-05-16T09:00:00Z',
        ]
        const isDayArray = [true, true, true, true, true]

        const result = findOptimalDepartureTimes(
            scoreMatrix,
            reasonMatrix,
            thunderMatrix,
            segmentDurations,
            timeSlotIsos,
            isDayArray,
            0,
            2,
            'BICYCLE',
        )

        expect(result.slots).toHaveLength(3)
        expect(result.bestSlotIndex).toBeGreaterThanOrEqual(0)
        expect(result.optimalWindowSummary.bestScore).toBeGreaterThan(70)
        expect(result.optimalWindowSummary.stableWindow).toContain('–')
    })

    it('applies FR-WETT-03 thunderstorm cap of max 50 points', () => {
        const scoreMatrix = [
            [95, 95],
            [95, 95],
        ]
        const reasonMatrix = [
            ['Gewitter', 'Gewitter'],
            ['Gewitter', 'Gewitter'],
        ]
        const thunderMatrix = [
            [true, true],
            [true, true],
        ]
        const segmentDurations = [15, 15]
        const timeSlotIsos = ['2026-05-16T14:00:00Z', '2026-05-16T14:15:00Z']
        const isDayArray = [true, true]

        const result = findOptimalDepartureTimes(
            scoreMatrix,
            reasonMatrix,
            thunderMatrix,
            segmentDurations,
            timeSlotIsos,
            isDayArray,
            0,
            1,
            'MOTORCYCLE',
        )

        for (const slot of result.slots) {
            expect(slot.hasThunderCap).toBe(true)
            expect(slot.routeScore).toBeLessThanOrEqual(50)
        }
    })

    it('applies daylight penalty when arrival or departure is at night', () => {
        const scoreMatrix = [
            [90, 90],
            [90, 90],
        ]
        const reasonMatrix = [
            ['Dunkel', 'Dunkel'],
            ['Dunkel', 'Dunkel'],
        ]
        const thunderMatrix = [
            [false, false],
            [false, false],
        ]
        const segmentDurations = [15, 15]
        const timeSlotIsos = ['2026-05-16T22:00:00Z', '2026-05-16T22:15:00Z']
        // Nightfall
        const isDayArray = [false, false]

        const hikeResult = findOptimalDepartureTimes(
            scoreMatrix,
            reasonMatrix,
            thunderMatrix,
            segmentDurations,
            timeSlotIsos,
            isDayArray,
            0,
            1,
            'HIKING',
        )

        // HIKING night multiplier is 0.25 -> 90 * 0.25 = ~23
        expect(hikeResult.slots[0]?.reachesNightfall).toBe(true)
        expect(hikeResult.slots[0]?.routeScore).toBeLessThan(30)
    })

    it('selects robust optimum via Gaussian smoothing rather than unstable spikes', () => {
        // Slot 0: 70
        // Slot 1: 90 (surrounded by 70 and 89)
        // Slot 2: 89 (surrounded by 90 and 92 -> stable plateau)
        // Slot 3: 92 (isolated spike followed by 40)
        // Slot 4: 40
        const scoreMatrix = [
            [50, 70, 90, 89, 92, 40, 40],
        ]
        const reasonMatrix = [
            ['A', 'A', 'A', 'A', 'A', 'A', 'A'],
        ]
        const thunderMatrix = [
            [false, false, false, false, false, false, false],
        ]
        const segmentDurations = [15]
        const timeSlotIsos = [
            '2026-05-16T07:45:00Z',
            '2026-05-16T08:00:00Z',
            '2026-05-16T08:15:00Z',
            '2026-05-16T08:30:00Z',
            '2026-05-16T08:45:00Z',
            '2026-05-16T09:00:00Z',
            '2026-05-16T09:15:00Z',
        ]
        const isDayArray = [true, true, true, true, true, true, true]

        const result = findOptimalDepartureTimes(
            scoreMatrix,
            reasonMatrix,
            thunderMatrix,
            segmentDurations,
            timeSlotIsos,
            isDayArray,
            0,
            4,
            'BICYCLE',
        )

        // Slot 0 (samples idx 1: 70): 0.2*70 + 0.6*70 + 0.2*90 = 74.0
        // Slot 1 (samples idx 2: 90): 0.2*70 + 0.6*90 + 0.2*89 = 85.8
        // Slot 2 (samples idx 3: 89): 0.2*90 + 0.6*89 + 0.2*92 = 89.8 (Highest robust score!)
        // Slot 3 (samples idx 4: 92): 0.2*89 + 0.6*92 + 0.2*40 = 81.0
        // Slot 4 (samples idx 5: 40): 0.2*92 + 0.6*40 + 0.2*40 = 50.4
        expect(result.bestSlotIndex).toBe(2)
    })
})
