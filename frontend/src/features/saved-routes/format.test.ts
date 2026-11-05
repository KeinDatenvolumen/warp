// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
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