// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-031] (dffcf3f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-038] (8b1a41d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-039] (b0345ea) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add application pages and routing (WARP-004)
// [KI-Log Nr. KI-041] (95411c2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: rebase to live (WARP-004)
// [KI-Log Nr. KI-043] (d6e73be) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: change capitalisation of file (WARP-004)
// [KI-Log Nr. KI-044] (d75389d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: clean up duplicate layout and routes and corrected capital letter (WARP-004)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {AuthProvider} from '../features/auth/AuthContext'
import AppRoutes from './routes'

export default function App() {
    return (
        <AuthProvider>
            <AppRoutes />
        </AuthProvider>
    )
}