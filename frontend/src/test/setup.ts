// [KI-Log Nr. KI-092] (474d299) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend registration tests (WARP-005)
// [KI-Log Nr. KI-113] (df21fe0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend registration tests (WARP-005)
// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import '@testing-library/jest-dom/vitest'
import {cleanup} from '@testing-library/react'
import {afterEach} from 'vitest'

afterEach(() => {
    cleanup()
})