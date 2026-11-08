// [KI-Log Nr. KI-167] (af1ca24) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: add frontend email verification (WARP-007)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {beforeEach, describe, expect, it, vi} from 'vitest'
import {confirmEmail, resendVerificationEmail} from './emailVerificationApi'
import {postJson} from './restClient'

vi.mock('./restClient', () => ({
    postJson: vi.fn()
}))

const mockedPostJson = vi.mocked(postJson)

describe('emailVerificationApi', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('bestätigt E-Mail über POST /api/v1/email-verifications/confirm mit CSRF', async () => {
        mockedPostJson.mockResolvedValueOnce(undefined)

        await confirmEmail('valid-token-123')

        expect(mockedPostJson).toHaveBeenCalledTimes(1)
        expect(mockedPostJson).toHaveBeenCalledWith(
            '/api/v1/email-verifications/confirm',
            {token: 'valid-token-123'},
            {csrf: true}
        )
    })

    it('sendet Verifikations-E-Mail erneut über POST /api/v1/email-verifications/resend mit CSRF', async () => {
        mockedPostJson.mockResolvedValueOnce(undefined)

        await resendVerificationEmail('user@example.de')

        expect(mockedPostJson).toHaveBeenCalledTimes(1)
        expect(mockedPostJson).toHaveBeenCalledWith(
            '/api/v1/email-verifications/resend',
            {email: 'user@example.de'},
            {csrf: true}
        )
    })
})
