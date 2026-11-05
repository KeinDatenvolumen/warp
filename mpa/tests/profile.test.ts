// [KI-Log Nr. KI-20] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Multi-Page Application mit Express & EJS (WARP-034)
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
    expect(res.text).toContain('usr-999')
    expect(res.text).toContain('GraphQL')
    expect(res.text).toContain('Bestätigt')
  })
})
