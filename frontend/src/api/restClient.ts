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

function parseErrorResponse(
    responseBody: string
): ApiErrorResponse {
    let parsed: unknown = responseBody

    // Bis zu zweimal parsen, falls JSON als String verpackt ist.
    for (let attempt = 0; attempt < 2; attempt++) {
        if (typeof parsed !== 'string') {
            break
        }

        try {
            parsed = JSON.parse(parsed)
        } catch {
            return {}
        }
    }

    if (
        typeof parsed === 'object' &&
        parsed !== null &&
        !Array.isArray(parsed)
    ) {
        return parsed as ApiErrorResponse
    }

    return {}
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

    const responseBody = await response.text()

    if (response.ok) {
        return JSON.parse(responseBody) as TResponse
    }

    const errorResponse = parseErrorResponse(responseBody)

    throw new ApiRequestError(
        response.status,
        errorResponse
    )
}