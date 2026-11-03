/**
 * @vitest-environment jsdom
 */

import {ApolloLink, execute, gql} from '@apollo/client'
import {Observable} from 'rxjs'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {sessionInvalidLink} from './apolloClient'

const notifySessionInvalid = vi.hoisted(() => vi.fn())

vi.mock('../features/auth/sessionEvents', () => ({
    notifySessionInvalid
}))

const query = gql`
    query CurrentUser {
        currentUser {
            id
        }
    }
`

function runWithResult(
    result: object,
    operationQuery = query,
    context?: Record<string, unknown>
): Promise<void> {
    const terminating = new ApolloLink(
        () => new Observable((subscriber) => {
            subscriber.next(result)
            subscriber.complete()
        })
    )

    return new Promise((resolve, reject) => {
        execute(
            ApolloLink.from([sessionInvalidLink, terminating]),
            {query: operationQuery, context},
            {client: {} as never}
        ).subscribe({
            complete: resolve,
            error: reject
        })
    })
}

describe('sessionInvalidLink', () => {
    afterEach(() => {
        notifySessionInvalid.mockClear()
    })

    it('meldet SESSION_INVALID bei normalen Operationen', async () => {
        await runWithResult({
            data: null,
            errors: [
                {
                    message: 'Die Sitzung ist ungültig.',
                    extensions: {code: 'SESSION_INVALID'}
                }
            ]
        })

        expect(notifySessionInvalid).toHaveBeenCalledTimes(1)
    })

    it('ignoriert CurrentSession-Anfragen (Initial-Check beim Start)', async () => {
        const currentSessionQuery = gql`
            query CurrentSession {
                currentSession {
                    userId
                }
            }
        `
        await runWithResult(
            {
                data: null,
                errors: [
                    {
                        message: 'Die Sitzung ist ungültig.',
                        extensions: {code: 'SESSION_INVALID'}
                    }
                ]
            },
            currentSessionQuery
        )

        expect(notifySessionInvalid).not.toHaveBeenCalled()
    })

    it('ignoriert Anfragen mit skipSessionInvalidNotification Context', async () => {
        await runWithResult(
            {
                data: null,
                errors: [
                    {
                        message: 'Die Sitzung ist ungültig.',
                        extensions: {code: 'SESSION_INVALID'}
                    }
                ]
            },
            query,
            {skipSessionInvalidNotification: true}
        )

        expect(notifySessionInvalid).not.toHaveBeenCalled()
    })

    it('ignoriert andere Fehler und erfolgreiche Antworten', async () => {
        await runWithResult({
            data: null,
            errors: [
                {
                    message: 'Fehler',
                    extensions: {code: 'INTERNAL_SERVER_ERROR'}
                }
            ]
        })
        await runWithResult({data: {currentUser: {id: 'u'}}})

        expect(notifySessionInvalid).not.toHaveBeenCalled()
    })
})
