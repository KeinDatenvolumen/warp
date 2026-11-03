export function extractGraphQlErrorCodes(
    source: unknown
): string[] {
    if (!source || typeof source !== 'object') {
        return []
    }

    const codes = new Set<string>()

    const inspectItem = (item: unknown): void => {
        if (!item || typeof item !== 'object') {
            return
        }

        const candidate = item as {
            code?: unknown
            errorCode?: unknown
            extensions?: {
                code?: unknown
                classification?: unknown
            }
        }

        if (typeof candidate.extensions?.code === 'string') {
            codes.add(candidate.extensions.code)
        }
        if (typeof candidate.extensions?.classification === 'string') {
            codes.add(candidate.extensions.classification)
        }
        if (typeof candidate.code === 'string') {
            codes.add(candidate.code)
        }
        if (typeof candidate.errorCode === 'string') {
            codes.add(candidate.errorCode)
        }
    }

    if (Array.isArray(source)) {
        source.forEach(inspectItem)
        return Array.from(codes)
    }

    // Check graphQLErrors (ApolloError property)
    const graphQLErrors = (source as { graphQLErrors?: unknown }).graphQLErrors
    if (Array.isArray(graphQLErrors)) {
        graphQLErrors.forEach(inspectItem)
    }

    // Check errors property (standard GraphQL response)
    const errors = (source as { errors?: unknown }).errors
    if (Array.isArray(errors)) {
        errors.forEach(inspectItem)
    }

    // Check networkError (ApolloError property, which can be ServerError with result.errors)
    const networkResultErrors = (
        source as {
            networkError?: {
                result?: {
                    errors?: unknown
                }
                statusCode?: number
            }
        }
    ).networkError?.result?.errors
    if (Array.isArray(networkResultErrors)) {
        networkResultErrors.forEach(inspectItem)
    }

    const statusCode = (
        source as { networkError?: { statusCode?: number } }
    ).networkError?.statusCode
    if (statusCode === 401) {
        codes.add('UNAUTHORIZED')
    }

    // Check candidate itself
    inspectItem(source)

    return Array.from(codes)
}

const SESSION_INVALID_CODES = new Set([
    'SESSION_INVALID',
    'NOT_AUTHENTICATED',
    'UNAUTHORIZED',
])

export function containsSessionInvalidCode(
    source: unknown
): boolean {
    return extractGraphQlErrorCodes(source).some((code) =>
        SESSION_INVALID_CODES.has(code)
    )
}

export function isApolloInternalUrlMessage(message: string): boolean {
    return (
        message.includes('go.apollo.dev') ||
        message.includes('Invariant Violation') ||
        message.startsWith('An error occurred! For more details')
    )
}

export function isNetworkError(source: unknown): boolean {
    if (!source || typeof source !== 'object') {
        return false
    }

    if (typeof navigator !== 'undefined' && navigator.onLine === false) {
        return true
    }

    const apolloNetworkError = (
        source as { networkError?: unknown }
    ).networkError
    if (apolloNetworkError) {
        return true
    }

    const name = (source as { name?: unknown }).name
    const message = (source as { message?: unknown }).message

    if (typeof message === 'string') {
        const lower = message.toLowerCase()
        if (
            lower.includes('failed to fetch') ||
            lower.includes('network error') ||
            lower.includes('load failed') ||
            lower.includes('networkerror') ||
            lower.includes('connection refused') ||
            lower.includes('econnrefused')
        ) {
            return true
        }
    }

    if (name === 'TypeError' && typeof message === 'string') {
        return message.toLowerCase().includes('fetch')
    }

    return false
}

export function extractGraphQlErrorMessage(source: unknown): string | null {
    if (!source || typeof source !== 'object') {
        return null
    }

    const candidates: unknown[] = []

    if (Array.isArray(source)) {
        candidates.push(...source)
    } else {
        const graphQLErrors = (source as { graphQLErrors?: unknown }).graphQLErrors
        if (Array.isArray(graphQLErrors)) {
            candidates.push(...graphQLErrors)
        }

        const errors = (source as { errors?: unknown }).errors
        if (Array.isArray(errors)) {
            candidates.push(...errors)
        }

        const networkResultErrors = (
            source as {
                networkError?: {
                    result?: {
                        errors?: unknown
                    }
                }
            }
        ).networkError?.result?.errors
        if (Array.isArray(networkResultErrors)) {
            candidates.push(...networkResultErrors)
        }
    }

    for (const item of candidates) {
        if (!item || typeof item !== 'object') continue
        const message = (item as { message?: unknown }).message
        if (
            typeof message === 'string' &&
            message.trim().length > 0 &&
            !isApolloInternalUrlMessage(message)
        ) {
            return message.trim()
        }
    }

    const directMessage = (source as { message?: unknown }).message
    if (
        typeof directMessage === 'string' &&
        directMessage.trim().length > 0 &&
        !isApolloInternalUrlMessage(directMessage)
    ) {
        return directMessage.trim()
    }

    return null
}
