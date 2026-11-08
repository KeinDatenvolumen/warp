// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-014] (4741a73) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add local dev environment with watcher (WARP-001)
// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-092] (474d299) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend registration tests (WARP-005)
// [KI-Log Nr. KI-095] (cda338d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: Vite proxy for BFF service and Docker development (WARP-005)
// [KI-Log Nr. KI-113] (df21fe0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend registration tests (WARP-005)
// [KI-Log Nr. KI-116] (f54bfe7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: Vite proxy for BFF service and Docker development (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {defineConfig} from 'vitest/config'
import react from '@vitejs/plugin-react'

const bffTarget = process.env.VITE_BFF_PROXY_TARGET ?? 'http://bff-service:8080'

export default defineConfig({
    plugins: [react()],
    test: {
        environment: 'jsdom',
        setupFiles: './src/test/setup.ts',
        globals: true,
        css: true
    },

    server: {
        host: '0.0.0.0',
        port: 5173,
        strictPort: true,
        watch: {
            usePolling: process.env.VITE_USE_POLLING === 'true',
            interval: 250
        },
        proxy: {
            '/api': {
                target: bffTarget,
                changeOrigin: true
            },
            '/graphql': {
                target: bffTarget,
                changeOrigin: true
            }
        }
    }
})