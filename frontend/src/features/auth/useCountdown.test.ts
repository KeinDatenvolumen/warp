// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
/**
 * @vitest-environment jsdom
 */

import {act, renderHook} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {useCountdown} from './useCountdown'

describe('useCountdown', () => {
    beforeEach(() => {
        vi.useFakeTimers()
    })

    afterEach(() => {
        vi.useRealTimers()
    })

    it('startet bei null', () => {
        const {result} = renderHook(() => useCountdown())

        expect(result.current.remainingSeconds).toBe(0)
    })

    it('zählt sekündlich bis null herunter', () => {
        const {result} = renderHook(() => useCountdown())

        act(() => {
            result.current.start(3)
        })
        expect(result.current.remainingSeconds).toBe(3)

        act(() => {
            vi.advanceTimersByTime(1000)
        })
        expect(result.current.remainingSeconds).toBe(2)

        act(() => {
            vi.advanceTimersByTime(2000)
        })
        expect(result.current.remainingSeconds).toBe(0)
    })

    it('bleibt nach Ablauf bei null', () => {
        const {result} = renderHook(() => useCountdown())

        act(() => {
            result.current.start(1)
        })
        act(() => {
            vi.advanceTimersByTime(5000)
        })

        expect(result.current.remainingSeconds).toBe(0)
    })
})