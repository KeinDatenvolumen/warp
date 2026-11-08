// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import { Router } from 'express'

export const healthRouter = Router()

healthRouter.get('/health', (_req, res) => {
  res.status(200).json({
    status: 'UP',
    service: 'warp-mpa',
    timestamp: new Date().toISOString(),
  })
})
