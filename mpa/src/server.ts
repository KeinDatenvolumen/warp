// [KI-Log Nr. KI-20] KI-generiert (Google Antigravity Gemini 3.8 Flash High) für serverseitig gerenderte MPA (WARP-034)
import { app } from './app.js'
import { config } from './config.js'

const port = config.port
const host = '0.0.0.0'

app.listen(port, host, () => {
  console.log(`WaRP MPA Server läuft auf http://${host}:${port}`)
  console.log(`BFF URL: ${config.bffUrl}`)
  console.log(`Routenplaner (SPA) URL: ${config.routePlannerUrl}`)
})
