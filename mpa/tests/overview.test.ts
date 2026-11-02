import request from 'supertest'
import { describe, expect, it, vi } from 'vitest'
import { app } from '../src/app.js'
import { bffClient } from '../src/services/bffClient.js'

describe('GET / (Übersichtsseite)', () => {
  it('sollte die Übersichtsseite mit Verlinkung zum Routenplaner rendern', async () => {
    vi.spyOn(bffClient, 'fetchOverviewData').mockResolvedValue({
      user: null,
      activityProfiles: [
        { code: 'BICYCLE', displayName: 'Fahrrad' },
        { code: 'MOTORCYCLE', displayName: 'Motorrad' },
        { code: 'HIKING', displayName: 'Wandern' },
      ],
    })

    const res = await request(app).get('/')
    expect(res.status).toBe(200)
    expect(res.text).toContain('Weather-Aware Route Planner')
    expect(res.text).toContain('Routenplaner')
    expect(res.text).toContain('Fahrrad')
    expect(res.text).toContain('Motorrad')
    expect(res.text).toContain('Wandern')
  })
})
