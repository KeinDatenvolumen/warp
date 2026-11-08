// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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