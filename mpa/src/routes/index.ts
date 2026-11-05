// [KI-Log Nr. KI-20] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Multi-Page Application mit Express & EJS (WARP-034)
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
