import {
    ApolloClient,
    ApolloLink,
    HttpLink,
    InMemoryCache
} from '@apollo/client'
import {tap} from 'rxjs'
import {notifySessionInvalid} from '../features/auth/sessionEvents'
import {BFF_BASE_URL} from './bffBaseUrl'
import {containsSessionInvalidCode} from './graphqlErrors'
import {csrfLink} from './csrfLink'

const httpLink = new HttpLink({
    uri: `${BFF_BASE_URL}/graphql`,
    credentials: 'include',
    headers: {
        Accept: 'application/graphql-response+json'
    }
})

export const sessionInvalidLink = new ApolloLink(
    (operation, forward) =>
        forward(operation).pipe(
            tap((result) => {
                if (
                    operation.operationName !== 'CurrentSession' &&
                    !operation.getContext().skipSessionInvalidNotification &&
                    containsSessionInvalidCode(result.errors)
                ) {
                    notifySessionInvalid()
                }
            })
        )
)

export const apolloClient = new ApolloClient({
    link: ApolloLink.from([sessionInvalidLink, csrfLink, httpLink]),
    cache: new InMemoryCache()
})

export function clearApolloCache(): Promise<void> {
    return apolloClient.clearStore().then(() => undefined)
}