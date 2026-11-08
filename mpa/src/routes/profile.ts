// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import { Router } from 'express'
import { config } from '../config.js'
import { requireAuth } from '../middleware/authMiddleware.js'
import { bffClient } from '../services/bffClient.js'

export const profileRouter = Router()

profileRouter.get('/profile', requireAuth, async (req, res, next) => {
  try {
    const sessionToken = req.sessionToken!
    const { user, session } = await bffClient.fetchUserProfile(sessionToken)

    if (!user || !session) {
      res.clearCookie(config.sessionCookieName, {
        httpOnly: true,
        secure: config.isProduction,
        sameSite: 'none',
        path: '/',
      })
      res.redirect('/login?msg=login_required')
      return
    }

    const formatDate = (isoString?: string): string => {
      if (!isoString) return '-'
      try {
        const date = new Date(isoString)
        return new Intl.DateTimeFormat('de-DE', {
          dateStyle: 'medium',
          timeStyle: 'short',
        }).format(date)
      } catch {
        return isoString
      }
    }

    res.render('profile', {
      title: 'Profil | WaRP',
      user,
      session,
      formattedCreatedAt: formatDate(user.createdAt),
      formattedExpiresAt: formatDate(session.expiresAt),
      formattedSessionCreatedAt: formatDate(session.createdAt),
    })
  } catch (error) {
    next(error)
  }
})
