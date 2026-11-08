// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import { Router } from 'express'
import { bffClient } from '../services/bffClient.js'

export const indexRouter = Router()

indexRouter.get('/', async (req, res, next) => {
  try {
    const { user, activityProfiles } = await bffClient.fetchOverviewData(
      req.sessionToken
    )

    res.render('overview', {
      title: 'Übersicht | WaRP',
      user: user ?? res.locals.currentUser,
      activityProfiles,
    })
  } catch (error) {
    next(error)
  }
})
