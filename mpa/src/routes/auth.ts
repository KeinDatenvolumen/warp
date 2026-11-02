import { Router } from 'express'
import { config } from '../config.js'
import { requireGuest } from '../middleware/authMiddleware.js'
import { bffClient } from '../services/bffClient.js'

export const authRouter = Router()

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

authRouter.get('/login', requireGuest, (req, res) => {
  const messageCode = req.query.msg as string | undefined
  const redirect = (req.query.redirect as string) || '/profile'

  let infoMessage: string | undefined
  let successMessage: string | undefined

  if (messageCode === 'login_required') {
    infoMessage = 'Bitte melde dich an, um auf diese Seite zuzugreifen.'
  } else if (messageCode === 'logged_out') {
    successMessage = 'Du hast dich erfolgreich abgemeldet.'
  } else if (messageCode === 'registered') {
    successMessage =
      'Dein Konto wurde erfolgreich erstellt! Du kannst dich jetzt anmelden.'
  }

  res.render('login', {
    title: 'Anmelden | WaRP',
    email: '',
    redirect,
    errorMessage: undefined,
    infoMessage,
    successMessage,
    fieldErrors: {},
  })
})

authRouter.post('/login', requireGuest, async (req, res) => {
  const { email, password, redirect } = req.body
  const trimmedEmail = (email ?? '').trim()
  const redirectTarget = redirect && redirect.startsWith('/') ? redirect : '/profile'

  const fieldErrors: Record<string, string> = {}
  if (!trimmedEmail) {
    fieldErrors.email = 'Bitte gib deine E-Mail-Adresse ein.'
  } else if (!EMAIL_REGEX.test(trimmedEmail)) {
    fieldErrors.email = 'Bitte gib eine gültige E-Mail-Adresse ein.'
  }

  if (!password) {
    fieldErrors.password = 'Bitte gib dein Passwort ein.'
  }

  if (Object.keys(fieldErrors).length > 0) {
    res.status(400).render('login', {
      title: 'Anmelden | WaRP',
      email: trimmedEmail,
      redirect: redirectTarget,
      errorMessage: 'Bitte überprüfe die markierten Eingabefelder.',
      infoMessage: undefined,
      successMessage: undefined,
      fieldErrors,
    })
    return
  }

  const result = await bffClient.login(trimmedEmail, password)

  if (result.success && result.sessionToken) {
    res.cookie(config.sessionCookieName, result.sessionToken, {
      httpOnly: true,
      secure: config.isProduction,
      sameSite: 'lax',
      path: '/',
      maxAge: 7 * 24 * 60 * 60 * 1000, // 7 Tage
    })

    res.redirect(redirectTarget)
    return
  }

  res.status(401).render('login', {
    title: 'Anmelden | WaRP',
    email: trimmedEmail,
    redirect: redirectTarget,
    errorMessage:
      result.errorMessage ||
      'Anmeldung fehlgeschlagen. Bitte prüfe deine Zugangsdaten.',
    infoMessage: undefined,
    successMessage: undefined,
    fieldErrors: {},
  })
})

authRouter.get('/register', requireGuest, (_req, res) => {
  res.render('register', {
    title: 'Registrieren | WaRP',
    values: { name: '', email: '' },
    errorMessage: undefined,
    fieldErrors: {},
  })
})

authRouter.post('/register', requireGuest, async (req, res) => {
  const { name, email, password, passwordConfirm } = req.body
  const trimmedName = (name ?? '').trim()
  const trimmedEmail = (email ?? '').trim()

  const fieldErrors: Record<string, string> = {}

  if (!trimmedName || trimmedName.length < 2) {
    fieldErrors.name = 'Der Name muss mindestens 2 Zeichen lang sein.'
  }

  if (!trimmedEmail) {
    fieldErrors.email = 'Bitte gib deine E-Mail-Adresse ein.'
  } else if (!EMAIL_REGEX.test(trimmedEmail)) {
    fieldErrors.email = 'Bitte gib eine gültige E-Mail-Adresse ein.'
  }

  if (!password) {
    fieldErrors.password = 'Bitte gib ein Passwort ein.'
  } else if (password.length < 12) {
    fieldErrors.password =
      'Das Passwort muss mindestens 12 Zeichen lang sein.'
  }

  if (password !== passwordConfirm) {
    fieldErrors.passwordConfirm = 'Die Passwörter stimmen nicht überein.'
  }

  if (Object.keys(fieldErrors).length > 0) {
    res.status(400).render('register', {
      title: 'Registrieren | WaRP',
      values: { name: trimmedName, email: trimmedEmail },
      errorMessage: 'Bitte korrigiere die markierten Fehler.',
      fieldErrors,
    })
    return
  }

  const result = await bffClient.register(trimmedName, trimmedEmail, password)

  if (result.success) {
    res.redirect('/login?msg=registered')
    return
  }

  res.status(400).render('register', {
    title: 'Registrieren | WaRP',
    values: { name: trimmedName, email: trimmedEmail },
    errorMessage:
      result.errorMessage ||
      'Die Registrierung konnte nicht abgeschlossen werden.',
    fieldErrors: result.fieldErrors ?? {},
  })
})

const handleLogout = async (req: any, res: any) => {
  const sessionToken = req.cookies?.[config.sessionCookieName]

  if (sessionToken) {
    await bffClient.logout(sessionToken)
  }

  res.clearCookie(config.sessionCookieName, {
    httpOnly: true,
    secure: config.isProduction,
    sameSite: 'lax',
    path: '/',
  })

  res.redirect('/login?msg=logged_out')
}

authRouter.get('/logout', handleLogout)
authRouter.post('/logout', handleLogout)
