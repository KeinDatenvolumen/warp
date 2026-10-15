export interface RegistrationRequest {
    name: string
    email: string
    password: string
}

export interface RegistrationResponse {
    id: string
    name: string
    email: string
    emailVerified: boolean
    createdAt: string
}