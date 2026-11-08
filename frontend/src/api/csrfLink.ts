// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {ApolloLink} from '@apollo/client'
import {Kind, OperationTypeNode, type DocumentNode} from 'graphql'
import {catchError, from, switchMap, throwError} from 'rxjs'
import {getCsrfToken} from './restClient'

type CsrfToken = Awaited<ReturnType<typeof getCsrfToken>>

function isMutation(query: DocumentNode): boolean {
    return query.definitions.some(
        (definition) =>
            definition.kind === Kind.OPERATION_DEFINITION &&
            definition.operation === OperationTypeNode.MUTATION
    )
}

function isForbidden(error: unknown): boolean {
    const candidate = error as {
        statusCode?: unknown
        status?: unknown
        response?: { status?: unknown }
    } | null

    return (
        candidate?.statusCode === 403 ||
        candidate?.status === 403 ||
        candidate?.response?.status === 403
    )
}

/**
 * GraphQL-Mutationen sind laut API-Vertrag durch CSRF geschützt.
 * Queries bleiben unverändert und laden keinen Token.
 */
export const csrfLink = new ApolloLink((operation, forward) => {
    if (!isMutation(operation.query)) {
        return forward(operation)
    }

    function sendWith(token: CsrfToken) {
        const headers = (operation.getContext().headers ?? {}) as Record<
            string,
            string
        >

        operation.setContext({
            headers: {...headers, [token.headerName]: token.token}
        })

        return forward(operation)
    }

    return from(getCsrfToken()).pipe(
        switchMap(sendWith),
        catchError((error: unknown) => {
            if (!isForbidden(error)) {
                return throwError(() => error)
            }

            // Token abgelaufen: einmal erneuern und wiederholen.
            return from(getCsrfToken(true)).pipe(switchMap(sendWith))
        })
    )
})