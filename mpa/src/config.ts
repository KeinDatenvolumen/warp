// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import dotenv from 'dotenv'

dotenv.config()

export const config = {
  port: Number.parseInt(process.env.PORT ?? '8080', 10),
  bffUrl: (process.env.BFF_URL ?? 'http://localhost:8080').replace(/\/+$/, ''),
  routePlannerUrl:
    process.env.ROUTE_PLANNER_URL ??
    (process.env.NODE_ENV === 'production'
      ? 'https://warp-frontend-mos-25-843b86c38c7b.herokuapp.com/route-planner'
      : 'http://localhost:5173/route-planner'),
  sessionCookieName: 'session',
  isProduction: process.env.NODE_ENV === 'production',
}
