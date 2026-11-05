// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
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