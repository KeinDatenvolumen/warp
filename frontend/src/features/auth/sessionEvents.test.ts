// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
import {describe, expect, it, vi} from 'vitest'
import {
    notifySessionInvalid,
    subscribeToSessionInvalid
} from './sessionEvents'

describe('sessionEvents', () => {
    it('benachrichtigt registrierte Listener', () => {
        const first = vi.fn()
        const second = vi.fn()
        const unsubscribeFirst = subscribeToSessionInvalid(first)
        const unsubscribeSecond = subscribeToSessionInvalid(second)

        notifySessionInvalid()

        expect(first).toHaveBeenCalledTimes(1)
        expect(second).toHaveBeenCalledTimes(1)

        unsubscribeFirst()
        unsubscribeSecond()
    })

    it('benachrichtigt abgemeldete Listener nicht mehr', () => {
        const listener = vi.fn()
        const unsubscribe = subscribeToSessionInvalid(listener)

        unsubscribe()
        notifySessionInvalid()

        expect(listener).not.toHaveBeenCalled()
    })
})