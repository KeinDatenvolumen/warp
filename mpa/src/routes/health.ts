// [KI-Log Nr. KI-20] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Multi-Page Application mit Express & EJS (WARP-034)
import { Router } from 'express'

export const healthRouter = Router()

healthRouter.get('/health', (_req, res) => {
  res.status(200).json({
    status: 'UP',
    service: 'warp-mpa',
    timestamp: new Date().toISOString(),
  })
})
