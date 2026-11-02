import path from 'node:path'
import { fileURLToPath } from 'node:url'
import cookieParser from 'cookie-parser'
import express from 'express'
import helmet from 'helmet'
import { attachUserMiddleware } from './middleware/authMiddleware.js'
import { authRouter } from './routes/auth.js'
import { healthRouter } from './routes/health.js'
import { indexRouter } from './routes/index.js'
import { profileRouter } from './routes/profile.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export const app = express()

// Views & Static
const viewsPath = path.resolve(process.cwd(), 'views')
const publicPath = path.resolve(process.cwd(), 'public')

app.set('view engine', 'ejs')
app.set('views', viewsPath)

// Security Headers
app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
        scriptSrc: ["'self'", "'unsafe-inline'"],
        imgSrc: ["'self'", 'data:', 'https:'],
        fontSrc: ["'self'"],
        connectSrc: ["'self'"],
      },
    },
  })
)

// Body & Cookie Parsers
app.use(express.urlencoded({ extended: true }))
app.use(express.json())
app.use(cookieParser())

// Static Assets
app.use(express.static(publicPath))

// User Context Middleware
app.use(attachUserMiddleware)

// Routes
app.use(healthRouter)
app.use(indexRouter)
app.use(authRouter)
app.use(profileRouter)

// 404 Handler
app.use((req, res) => {
  res.status(404).render('error', {
    title: '404 - Seite nicht gefunden | WaRP',
    statusCode: 404,
    message: 'Die angeforderte Seite existiert nicht.',
  })
})

// Global Error Handler
app.use(
  (
    err: any,
    _req: express.Request,
    res: express.Response,
    _next: express.NextFunction
  ) => {
    console.error('Anwendungsfehler:', err)
    res.status(err.status || 500).render('error', {
      title: 'Fehler | WaRP',
      statusCode: err.status || 500,
      message:
        err.message ||
        'Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es später noch einmal.',
    })
  }
)
