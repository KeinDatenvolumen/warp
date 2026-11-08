// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {createServer} from 'node:http'
import {createReadStream, existsSync, statSync} from 'node:fs'
import {extname, join, normalize} from 'node:path'
import {fileURLToPath} from 'node:url'

const port = Number.parseInt(process.env.PORT ?? '8080', 10)
const host = '0.0.0.0'
const directory = fileURLToPath(new URL('./dist', import.meta.url))

const contentTypes = {
    '.css': 'text/css; charset=utf-8',
    '.html': 'text/html; charset=utf-8',
    '.ico': 'image/x-icon',
    '.js': 'text/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.svg': 'image/svg+xml',
    '.webp': 'image/webp'
}

const server = createServer((request, response) => {
    if (request.url === '/health') {
        response.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Cache-Control': 'no-store'
        })
        response.end(JSON.stringify({ status: 'UP' }))
        return
    }

    const requestPath = request.url?.split('?')[0] ?? '/'
    const safePath = normalize(requestPath).replace(/^(\.\.[/\\])+/, '')
    let filePath = join(directory, safePath)

    if (
        requestPath === '/' ||
        !existsSync(filePath) ||
        !statSync(filePath).isFile()
    ) {
        filePath = join(directory, 'index.html')
    }

    const extension = extname(filePath)
    const contentType =
        contentTypes[extension] ?? 'application/octet-stream'

    response.writeHead(200, {
        'Content-Type': contentType,
        'X-Content-Type-Options': 'nosniff',
        'X-Frame-Options': 'DENY',
        'Referrer-Policy': 'strict-origin-when-cross-origin'
    })

    createReadStream(filePath).pipe(response)
})

server.listen(port, host, () => {
    console.log(`WaRP frontend listening on port ${port}`)
})