// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
import request from 'supertest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { app } from '../src/app.js'
import { bffClient } from '../src/services/bffClient.js'

describe('GET /profile (Profilseite)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('sollte unauthentifizierte Anfragen zum Login weiterleiten', async () => {
    const res = await request(app).get('/profile')
    expect(res.status).toBe(302)
    expect(res.header.location).toContain('/login?redirect=%2Fprofile')
  })

  it('sollte für authentifizierte Benutzer das Profil per GraphQL rendern', async () => {
    vi.spyOn(bffClient, 'fetchUserProfile').mockResolvedValue({
      user: {
        id: 'usr-999',
        name: 'Erika Musterfrau',
        email: 'erika@example.com',
        emailVerified: true,
        createdAt: '2026-01-15T10:00:00Z',
      },
      session: {
        userId: 'usr-999',
        createdAt: '2026-11-02T12:00:00Z',
        expiresAt: '2026-11-09T12:00:00Z',
      },
    })

    const res = await request(app)
      .get('/profile')
      .set('Cookie', ['session=valid-session-token'])

    expect(res.status).toBe(200)
    expect(res.text).toContain('Erika Musterfrau')
    expect(res.text).toContain('erika@example.com')
    expect(res.text).not.toContain('usr-999')
    expect(res.text).not.toContain('GraphQL')
    expect(res.text).toContain('Bestätigt')
  })
})
