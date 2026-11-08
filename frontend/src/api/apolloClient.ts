// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-121] (34d35e2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: detect invalid sessions globally in the GraphQL client (WARP-006)
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-234] (d6e6dbb) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): fix(frontend): prevent sessionInvalidLink and clearStore from cancelling startup queries when unauthenticated (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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