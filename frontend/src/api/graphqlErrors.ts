// [KI-Log Nr. KI-120] (61da337) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add session API with CSRF handling and Retry-After support (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export function extractGraphQlErrorCodes(
    source: unknown
): string[] {
    const errors = Array.isArray(source)
        ? source
        : (source as { errors?: unknown } | null | undefined)?.errors

    if (!Array.isArray(errors)) {
        return []
    }

    return errors.flatMap((error: unknown) => {
        const code = (
            error as { extensions?: { code?: unknown } } | null
        )?.extensions?.code

        return typeof code === 'string' ? [code] : []
    })
}

const SESSION_INVALID_CODES = new Set([
    'SESSION_INVALID',
    'NOT_AUTHENTICATED'
])

export function containsSessionInvalidCode(
    source: unknown
): boolean {
    return extractGraphQlErrorCodes(source).some((code) =>
        SESSION_INVALID_CODES.has(code)
    )
}