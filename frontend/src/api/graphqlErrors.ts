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