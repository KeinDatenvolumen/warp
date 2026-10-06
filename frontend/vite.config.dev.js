import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const bffTarget = process.env.VITE_BFF_PROXY_TARGET ?? 'http://bff-service:8080'

export default defineConfig({
    plugins: [react()],
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
            }
        }
    }
})
