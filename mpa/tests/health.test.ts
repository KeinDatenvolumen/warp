import request from 'supertest'
import { describe, expect, it } from 'vitest'
import { app } from '../src/app.js'

describe('GET /health', () => {
  it('sollte 200 OK und Status UP liefern', async () => {
    const res = await request(app).get('/health')
    expect(res.status).toBe(200)
    expect(res.body.status).toBe('UP')
    expect(res.body.service).toBe('warp-mpa')
    expect(res.body.timestamp).toBeDefined()
  })
})
