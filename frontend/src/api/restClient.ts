// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
import type {
    ApiErrorResponse,
    ApiFieldError
} from '../types/apiError'
import {BFF_BASE_URL} from './bffBaseUrl'

const MAX_RETRY_AFTER_SECONDS = 3600

export class ApiRequestError extends Error {
    readonly status: number
    readonly code?: string
    readonly fieldErrors: ApiFieldError[]
    readonly retryAfterSeconds?: number

    constructor(
        status: number,
        response: ApiErrorResponse,
        retryAfterSeconds?: number
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
        this.retryAfterSeconds = retryAfterSeconds
    }
}
export function parseRetryAfter(
    headerValue: string | null | undefined,
    now: number = Date.now()
): number | undefined {
    if (!headerValue) {
        return undefined
    }

    const value = headerValue.trim()
    let seconds: number

    if (/^\d+$/.test(value)) {
        seconds = Number.parseInt(value, 10)
    } else {
        const timestamp = Date.parse(value)

        if (Number.isNaN(timestamp)) {
            return undefined
        }

        seconds = Math.ceil((timestamp - now) / 1000)
    }

    if (seconds <= 0) {
        return undefined
    }

    return Math.min(seconds, MAX_RETRY_AFTER_SECONDS)
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

function toApiRequestError(
    response: Response,
    responseBody: string
): ApiRequestError {
    return new ApiRequestError(
        response.status,
        parseErrorResponse(responseBody),
        parseRetryAfter(response.headers.get('Retry-After'))
    )
}

interface CsrfToken {
    headerName: string
    token: string
}

let cachedCsrfToken: CsrfToken | undefined
let pendingCsrfToken: Promise<CsrfToken> | undefined

export function resetCsrfTokenCache(): void {
    cachedCsrfToken = undefined
    pendingCsrfToken = undefined
}

async function loadCsrfToken(): Promise<CsrfToken> {
    const response = await fetch(
        `${BFF_BASE_URL}/api/v1/csrf`,
        {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json'
            }
        }
    )

    const responseBody = await response.text()

    if (!response.ok) {
        throw toApiRequestError(response, responseBody)
    }

    const parsed = JSON.parse(responseBody) as Partial<CsrfToken>

    if (
        typeof parsed.headerName !== 'string' ||
        typeof parsed.token !== 'string'
    ) {
        throw new Error('Der CSRF-Token ist ungültig.')
    }

    return {
        headerName: parsed.headerName,
        token: parsed.token
    }
}

export async function getCsrfToken(
    forceRefresh = false
): Promise<CsrfToken> {
    if (!forceRefresh && cachedCsrfToken) {
        return cachedCsrfToken
    }

    if (!pendingCsrfToken) {
        pendingCsrfToken = loadCsrfToken()
            .then((token) => {
                cachedCsrfToken = token
                return token
            })
            .finally(() => {
                pendingCsrfToken = undefined
            })
    }

    return pendingCsrfToken
}

export interface RequestOptions {
    csrf?: boolean
}

type HttpMethod = 'POST' | 'DELETE'

function send(
    method: HttpMethod,
    path: string,
    body: unknown,
    csrfToken: CsrfToken | undefined
): Promise<Response> {
    const headers: Record<string, string> = {
        Accept: 'application/json'
    }
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json'
    }
    if (csrfToken) {
        headers[csrfToken.headerName] = csrfToken.token
    }
    return fetch(`${BFF_BASE_URL}${path}`, {
        method,
        credentials: 'include',
        headers,
        body: body === undefined
            ? undefined
            : JSON.stringify(body)
    })
}

async function execute(
    method: HttpMethod,
    path: string,
    body: unknown,
    options: RequestOptions
): Promise<string> {
    let response = await send(
        method,
        path,
        body,
        options.csrf ? await getCsrfToken() : undefined
    )

    if (options.csrf && response.status === 403) {
        response = await send(
            method,
            path,
            body,
            await getCsrfToken(true)
        )
    }

    const responseBody = await response.text()

    if (response.ok) {
        return responseBody
    }

    throw toApiRequestError(response, responseBody)
}

export async function postJson<TRequest, TResponse>(
    path: string,
    body: TRequest,
    options: RequestOptions = {}
): Promise<TResponse> {
    const responseBody = await execute(
        'POST',
        path,
        body,
        options
    )

    return (
        responseBody ? JSON.parse(responseBody) : undefined
    ) as TResponse
}

export async function deleteRequest(
    path: string,
    options: RequestOptions = {csrf: true}
): Promise<void> {
    await execute('DELETE', path, undefined, options)
}