const AUTH_STORAGE_KEY = 'warp-user-authenticated'

export function readAuthenticatedState(): boolean {
    return localStorage.getItem(AUTH_STORAGE_KEY) === 'true'
}

export function writeAuthenticatedState(
    isAuthenticated: boolean
): void {
    localStorage.setItem(
        AUTH_STORAGE_KEY,
        String(isAuthenticated)
    )
}

export function clearAuthenticatedState(): void {
    localStorage.removeItem(AUTH_STORAGE_KEY)
}