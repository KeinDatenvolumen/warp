// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
import { describe, expect, it } from 'vitest'
import { isLatestRequest } from './latestRequest'

describe('isLatestRequest', () => {
    it('accepts the newest request', () => {
        expect(isLatestRequest(3, 3)).toBe(true)
    })

    it('rejects an older request', () => {
        expect(isLatestRequest(2, 3)).toBe(false)
    })

    it('rejects a response when a newer request already exists', () => {
        const oldRequestId = 4
        const currentRequestId = 5

        expect(
            isLatestRequest(oldRequestId, currentRequestId),
        ).toBe(false)
    })
})