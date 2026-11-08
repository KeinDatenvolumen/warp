// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {
    beforeEach,
    describe,
    expect,
    it,
    vi
} from 'vitest'
import {
    confirmPasswordReset,
    sendPasswordResetEmail
} from './passwordResetApi'
import {postJson} from './restClient'

vi.mock('./restClient', () => ({
    postJson: vi.fn()
}))

const mockedPostJson = vi.mocked(postJson)

describe('passwordResetApi', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it(
        'fordert eine Passwort-Rücksetzungs-E-Mail '
        + 'über den BFF mit CSRF an',
        async () => {
            mockedPostJson.mockResolvedValueOnce(undefined)

            await sendPasswordResetEmail(
                'max@example.de'
            )

            expect(mockedPostJson)
                .toHaveBeenCalledTimes(1)

            expect(mockedPostJson)
                .toHaveBeenCalledWith(
                    '/api/v1/password-resets/send',
                    {
                        email: 'max@example.de'
                    },
                    {
                        csrf: true
                    }
                )
        }
    )

    it(
        'bestätigt die Passwort-Rücksetzung '
        + 'über den BFF mit CSRF',
        async () => {
            mockedPostJson.mockResolvedValueOnce(undefined)

            await confirmPasswordReset(
                'gueltiges-reset-token',
                'MeinNeuesPasswort123'
            )

            expect(mockedPostJson)
                .toHaveBeenCalledTimes(1)

            expect(mockedPostJson)
                .toHaveBeenCalledWith(
                    '/api/v1/password-resets/confirm',
                    {
                        token: 'gueltiges-reset-token',
                        newPassword:
                            'MeinNeuesPasswort123'
                    },
                    {
                        csrf: true
                    }
                )
        }
    )
})