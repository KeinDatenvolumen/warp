// [KI-Log Nr. KI-125] (d8c8a50) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add login validation, error mapping, redirect target and countdown helper (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export interface LoginValues {
    email: string
    password: string
}

export interface LoginFieldErrors {
    email?: string
    password?: string
}

export const LOGIN_FIELD_ORDER = ['email', 'password'] as const

export function validateLogin(
    values: LoginValues
): LoginFieldErrors {
    const errors: LoginFieldErrors = {}
    const email = values.email.trim()

    if (!email) {
        errors.email = 'Bitte gib deine E-Mail-Adresse ein.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errors.email = 'Bitte gib eine gültige E-Mail-Adresse ein.'
    }

    if (values.password.length === 0) {
        errors.password = 'Bitte gib dein Passwort ein.'
    }

    return errors
}