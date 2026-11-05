// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
import {defineConfig} from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    test: {
        environment: 'jsdom',
        setupFiles: './src/test/setup.ts',
        globals: true,
        css: true
    },
    server: {
        port: 5173,
        strictPort: true,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/graphql': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },
})