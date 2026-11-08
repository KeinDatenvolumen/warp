// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-031] (dffcf3f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-038] (8b1a41d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-041] (95411c2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: rebase to live (WARP-004)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-206] (d2ddaae) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add theme structure with three color profiles and light/dark mode (WARP-033)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {ApolloProvider} from '@apollo/client/react'
import {BrowserRouter} from 'react-router'
import {apolloClient} from '../api/apolloClient'
import {ThemeProvider} from '../features/theme/ThemeContext'
import App from './App'

import '../styles/reset.css'
import '../styles/variables.css'
import '../styles/themes.css'
import '../styles/typography.css'
import '../styles/global.css'

const rootElement = document.getElementById('root')

if (!rootElement) {
    throw new Error('Das Root-Element wurde nicht gefunden.')
}

createRoot(rootElement).render(
    <StrictMode>
        <ThemeProvider>
            <ApolloProvider client={apolloClient}>
                <BrowserRouter>
                    <App />
                </BrowserRouter>
            </ApolloProvider>
        </ThemeProvider>
    </StrictMode>,
)