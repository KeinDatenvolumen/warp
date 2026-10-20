export const LOGIN_PATH = '/login'

export const DEFAULT_AUTHENTICATED_PATH = '/route-planner'

export const LOGOUT_REDIRECT_PATH = '/'

const EXCLUDED_PATHS = new Set([LOGIN_PATH, '/register'])

interface RedirectLocation {
    pathname?: unknown
    search?: unknown
    hash?: unknown
}

export function resolveRedirectTarget(state: unknown): string {
    const from = (state as { from?: RedirectLocation } | null)?.from
    const pathname = from?.pathname

    if (
        typeof pathname !== 'string' ||
        !pathname.startsWith('/') ||
        pathname.startsWith('//') ||
        EXCLUDED_PATHS.has(pathname)
    ) {
        return DEFAULT_AUTHENTICATED_PATH
    }

    const search = typeof from?.search === 'string' ? from.search : ''
    const hash = typeof from?.hash === 'string' ? from.hash : ''

    return `${pathname}${search}${hash}`
}