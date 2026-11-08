// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import type { Request, Response, NextFunction } from 'express'
import { config } from '../config.js'
import { bffClient } from '../services/bffClient.js'
import type { CurrentSession, User } from '../services/types.js'

// Erweitere Express Request Interface
declare global {
  namespace Express {
    interface Request {
      user?: User | null
      sessionInfo?: CurrentSession | null
      sessionToken?: string
    }
  }
}

export async function attachUserMiddleware(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  const sessionToken = req.cookies?.[config.sessionCookieName]

  res.locals.routePlannerUrl = config.routePlannerUrl
  res.locals.currentPath = req.path

  if (!sessionToken) {
    req.user = null
    req.sessionInfo = null
    res.locals.currentUser = null
    res.locals.isAuthenticated = false
    next()
    return
  }

  req.sessionToken = sessionToken

  try {
    const { user, session } = await bffClient.fetchUserProfile(sessionToken)
    if (user && session) {
      req.user = user
      req.sessionInfo = session
      res.locals.currentUser = user
      res.locals.sessionInfo = session
      res.locals.isAuthenticated = true
    } else {
      req.user = null
      req.sessionInfo = null
      res.locals.currentUser = null
      res.locals.isAuthenticated = false
    }
  } catch {
    req.user = null
    req.sessionInfo = null
    res.locals.currentUser = null
    res.locals.isAuthenticated = false
  }

  next()
}

export function requireAuth(
  req: Request,
  res: Response,
  next: NextFunction
): void {
  if (!res.locals.isAuthenticated || !req.user) {
    const redirectUrl = encodeURIComponent(req.originalUrl || '/profile')
    res.redirect(`/login?redirect=${redirectUrl}&msg=login_required`)
    return
  }
  next()
}

export function requireGuest(
  req: Request,
  res: Response,
  next: NextFunction
): void {
  if (res.locals.isAuthenticated && req.user) {
    res.redirect('/profile')
    return
  }
  next()
}
