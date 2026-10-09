const AUTH_STORAGE_KEY = 'warp-user-authenticated'

export function readAuthenticatedState() {
    return localStorage.getItem(AUTH_STORAGE_KEY) === 'true'
}

export function writeAuthenticatedState(isAuthenticated) {
    localStorage.setItem(
        AUTH_STORAGE_KEY,
        String(isAuthenticated)
    )
}

export function clearAuthenticatedState() {
    localStorage.removeItem(AUTH_STORAGE_KEY)
}