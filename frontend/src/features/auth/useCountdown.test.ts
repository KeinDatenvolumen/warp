// [KI-Log Nr. KI-125] (d8c8a50) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add login validation, error mapping, redirect target and countdown helper (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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