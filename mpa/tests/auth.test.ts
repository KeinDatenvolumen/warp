// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import request from 'supertest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { app } from '../src/app.js'
import { bffClient } from '../src/services/bffClient.js'

describe('Auth Routen (Login, Registrierung, Logout)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  describe('GET /login', () => {
    it('sollte das Anmeldeformular rendern', async () => {
      const res = await request(app).get('/login')
      expect(res.status).toBe(200)
      expect(res.text).toContain('Willkommen zurück')
      expect(res.text).toContain('name="email"')
      expect(res.text).toContain('name="password"')
    })
  })

  describe('POST /login', () => {
    it('sollte Fehler anzeigen bei leeren Feldern', async () => {
      const res = await request(app)
        .post('/login')
        .type('form')
        .send({ email: '', password: '' })

      expect(res.status).toBe(400)
      expect(res.text).toContain('Bitte gib deine E-Mail-Adresse ein')
    })

    it('sollte bei erfolgreicher Anmeldung ein Cookie setzen und weiterleiten', async () => {
      vi.spyOn(bffClient, 'login').mockResolvedValue({
        success: true,
        sessionToken: 'test-session-token-12345',
        userId: 'user-1',
      })

      const res = await request(app)
        .post('/login')
        .type('form')
        .send({ email: 'test@example.com', password: 'ValidPassword123' })

      expect(res.status).toBe(302)
      expect(res.header.location).toBe('/profile')
      const cookies = res.header['set-cookie']
      expect(cookies).toBeDefined()
      expect(cookies[0]).toContain('session=test-session-token-12345')
    })

    it('sollte Fehlermeldung anzeigen bei falschen Anmeldedaten', async () => {
      vi.spyOn(bffClient, 'login').mockResolvedValue({
        success: false,
        errorMessage: 'Die angegebene E-Mail-Adresse oder das Passwort ist falsch.',
      })

      const res = await request(app)
        .post('/login')
        .type('form')
        .send({ email: 'wrong@example.com', password: 'WrongPassword123' })

      expect(res.status).toBe(401)
      expect(res.text).toContain('Die angegebene E-Mail-Adresse oder das Passwort ist falsch.')
    })
  })

  describe('GET /register', () => {
    it('sollte das Registrierungsformular anzeigen', async () => {
      const res = await request(app).get('/register')
      expect(res.status).toBe(200)
      expect(res.text).toContain('Konto erstellen')
      expect(res.text).toContain('name="name"')
      expect(res.text).toContain('name="email"')
      expect(res.text).toContain('name="password"')
    })
  })

  describe('POST /register', () => {
    it('sollte Validierungsfehler bei zu kurzem Passwort anzeigen', async () => {
      const res = await request(app)
        .post('/register')
        .type('form')
        .send({
          name: 'Max Mustermann',
          email: 'max@example.com',
          password: 'short',
          passwordConfirm: 'short',
        })

      expect(res.status).toBe(400)
      expect(res.text).toContain('mindestens 12 Zeichen')
    })

    it('sollte bei erfolgreicher Registrierung zum Login weiterleiten', async () => {
      vi.spyOn(bffClient, 'register').mockResolvedValue({
        success: true,
        userId: 'user-new-id',
      })

      const res = await request(app)
        .post('/register')
        .type('form')
        .send({
          name: 'Max Mustermann',
          email: 'max@example.com',
          password: 'SuperSecurePassword123!',
          passwordConfirm: 'SuperSecurePassword123!',
        })

      expect(res.status).toBe(302)
      expect(res.header.location).toBe('/login?msg=registered')
    })
  })

  describe('POST /logout', () => {
    it('sollte das Sitzungscookie löschen und zum Login weiterleiten', async () => {
      vi.spyOn(bffClient, 'logout').mockResolvedValue(undefined)

      const res = await request(app)
        .post('/logout')
        .set('Cookie', ['session=test-token'])

      expect(res.status).toBe(302)
      expect(res.header.location).toBe('/login?msg=logged_out')
      const cookies = res.header['set-cookie']
      expect(cookies[0]).toContain('session=;')
    })
  })
})
