// [KI-Log Nr. KI-121] (34d35e2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: detect invalid sessions globally in the GraphQL client (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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