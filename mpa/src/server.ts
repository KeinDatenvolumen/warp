// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import { app } from './app.js'
import { config } from './config.js'

const port = config.port
const host = '0.0.0.0'

app.listen(port, host, () => {
  console.log(`WaRP MPA Server läuft auf http://${host}:${port}`)
  console.log(`BFF URL: ${config.bffUrl}`)
  console.log(`Routenplaner (SPA) URL: ${config.routePlannerUrl}`)
})
