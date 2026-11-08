// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {
    formatDate,
    formatDateTime,
    formatDistance,
    formatDuration
} from './format'

describe('format', () => {
    it('formatiert Distanzen', () => {
        expect(formatDistance(850)).toBe('850 m')
        expect(formatDistance(57600)).toBe('57,6 km')
    })

    it('formatiert Dauern', () => {
        expect(formatDuration(1800)).toBe('30 Min.')
        expect(formatDuration(11520)).toBe('3 Std. 12 Min.')
    })

    it('formatiert Zeitpunkte und fängt ungültige Werte ab', () => {
        expect(formatDate('2026-10-21T10:00:00Z')).toContain('2026')
        expect(formatDateTime('2026-10-21T10:00:00Z')).toContain('2026')
        expect(formatDate('kein-datum')).toBe('–')
    })
})