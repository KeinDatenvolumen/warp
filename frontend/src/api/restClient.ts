import type {
    ApiErrorResponse,
    ApiFieldError
} from '../types/apiError'

const BFF_BASE_URL = (
    import.meta.env.VITE_BFF_BASE_URL ?? ''
).replace(/\/$/, '')

export class ApiRequestError extends Error {
    readonly status: number
    readonly code?: string
    readonly fieldErrors: ApiFieldError[]

    constructor(
        status: number,
        response: ApiErrorResponse
    ) {
        super(
            response.detail ??
            response.message ??
            'Die Anfrage konnte nicht verarbeitet werden.'
        )

        this.name = 'ApiRequestError'
        this.status = status
        this.code = response.code
        this.fieldErrors = response.fieldErrors ?? []
    }
}

export async function postJson<TRequest, TResponse>(
    path: string,
    body: TRequest
): Promise<TResponse> {
    const response = await fetch(
        `${BFF_BASE_URL}${path}`,
        {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: JSON.stringify(body)
        }
    )

    if (response.ok) {
        return response.json() as Promise<TResponse>
    }

    let errorResponse: ApiErrorResponse = {}

    try {
        errorResponse = await response.json()
    } catch {
        // Fehlerantwort war kein gültiges JSON.
    }

    throw new ApiRequestError(
        response.status,
        errorResponse
    )
}