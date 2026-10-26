/**
 * @vitest-environment jsdom
 */

import {ApolloLink, execute, gql} from '@apollo/client'
import type {DocumentNode} from 'graphql'
import {Observable} from 'rxjs'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {csrfLink} from './csrfLink'
import {getCsrfToken} from './restClient'

vi.mock('./restClient', () => ({getCsrfToken: vi.fn()}))

const mockedGetCsrfToken = vi.mocked(getCsrfToken)

const mutation = gql`
    mutation DeleteSavedRoute($id: ID!) {
        deleteSavedRoute(id: $id) {
            deletedRouteId
        }
    }
`

const query = gql`
    query CurrentSession {
        currentSession {
            userId
        }
    }
`

function token(value: string) {
    return {headerName: 'X-XSRF-TOKEN', token: value}
}

function run(
    document: DocumentNode,
    respond: (headers: Record<string, string>) => Observable<object>
): Promise<void> {
    const terminating = new ApolloLink((operation) =>
        respond(
            (operation.getContext().headers ?? {}) as Record<string, string>
        )
    )

    return new Promise((resolve, reject) => {
        execute(
            ApolloLink.from([csrfLink, terminating]),
            {query: document, variables: {id: 'route_1'}},
            {client: {} as never}
        ).subscribe({complete: resolve, error: reject})
    })
}

function success(): Observable<object> {
    return new Observable((subscriber) => {
        subscriber.next({data: {}})
        subscriber.complete()
    })
}

describe('csrfLink', () => {
    beforeEach(() => {
        mockedGetCsrfToken.mockReset()
    })

    it('sendet den CSRF-Token bei Mutationen', async () => {
        mockedGetCsrfToken.mockResolvedValue(token('token-1'))
        let sentHeaders: Record<string, string> = {}

        await run(mutation, (headers) => {
            sentHeaders = headers
            return success()
        })

        expect(sentHeaders['X-XSRF-TOKEN']).toBe('token-1')
    })

    it('lädt bei Queries keinen Token', async () => {
        await run(query, () => success())

        expect(mockedGetCsrfToken).not.toHaveBeenCalled()
    })

    it('erneuert den Token bei 403 einmal und wiederholt', async () => {
        mockedGetCsrfToken
            .mockResolvedValueOnce(token('token-1'))
            .mockResolvedValueOnce(token('token-2'))
        const sentTokens: string[] = []

        await run(mutation, (headers) => {
            sentTokens.push(headers['X-XSRF-TOKEN'] ?? '')

            if (sentTokens.length === 1) {
                return new Observable((subscriber) =>
                    subscriber.error({statusCode: 403})
                )
            }

            return success()
        })

        expect(sentTokens).toEqual(['token-1', 'token-2'])
        expect(mockedGetCsrfToken).toHaveBeenNthCalledWith(2, true)
    })

    it('wiederholt nicht bei anderen Fehlern', async () => {
        mockedGetCsrfToken.mockResolvedValue(token('token-1'))

        await expect(
            run(mutation, () =>
                new Observable((subscriber) =>
                    subscriber.error({statusCode: 500})
                )
            )
        ).rejects.toEqual({statusCode: 500})

        expect(mockedGetCsrfToken).toHaveBeenCalledTimes(1)
    })
})