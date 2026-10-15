export interface ApiFieldError {
    field?: string
    code?: string
    message?: string
}

export interface ApiErrorResponse {
    status?: number
    code?: string
    detail?: string
    message?: string
    fieldErrors?: ApiFieldError[]
}