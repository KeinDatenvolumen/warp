// [KI-Log Nr. KI-204] (80d6d17) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add account api, types and validation for the profile (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {changeEmail, changePassword} from './accountApi'
import {postJson} from './restClient'

vi.mock('./restClient', () => ({
    postJson: vi.fn()
}))

describe('accountApi', () => {
    beforeEach(() => {
        vi.mocked(postJson).mockReset()
        vi.mocked(postJson).mockResolvedValue(undefined)
    })

    it('sendet die Passwortänderung mit CSRF-Token', async () => {
        const request = {
            currentPassword: 'alt-passwort-123',
            newPassword: 'neues-passwort-123'
        }

        await changePassword(request)

        expect(postJson).toHaveBeenCalledWith(
            '/api/v1/users/me/password-changes',
            request,
            {csrf: true}
        )
    })

    it('sendet die E-Mail-Änderung mit CSRF-Token', async () => {
        const request = {newEmail: 'neu@example.de', password: 'geheim'}

        await changeEmail(request)

        expect(postJson).toHaveBeenCalledWith(
            '/api/v1/users/me/email-changes',
            request,
            {csrf: true}
        )
    })
})