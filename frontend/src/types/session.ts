export interface LoginRequest {
    email: string
    password: string
}

export interface CreateSessionResponse {
    userId: string
    expiresAt: string
}

export interface CurrentSession {
    userId: string
    createdAt: string
    expiresAt: string
}