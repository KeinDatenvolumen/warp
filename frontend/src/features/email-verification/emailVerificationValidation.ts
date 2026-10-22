export function validateResendEmail(email: string): string | undefined {
    const trimmed = email.trim()

    if (!trimmed) {
        return 'Bitte gib deine E-Mail-Adresse ein.'
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
        return 'Bitte gib eine gültige E-Mail-Adresse ein.'
    }

    return undefined
}
