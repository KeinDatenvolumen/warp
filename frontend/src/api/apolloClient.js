import {
    ApolloClient,
    HttpLink,
    InMemoryCache
} from '@apollo/client'

const BFF_BASE_URL = (
    import.meta.env.VITE_BFF_BASE_URL ?? ''
).replace(/\/$/, '')

const httpLink = new HttpLink({
    uri: `${BFF_BASE_URL}/graphql`,
    credentials: 'include',
    headers: {
        Accept: 'application/graphql-response+json'
    }
})

export const apolloClient = new ApolloClient({
    link: httpLink,
    cache: new InMemoryCache(),
    defaultOptions: {
        watchQuery: {
            errorPolicy: 'all'
        },
        query: {
            errorPolicy: 'all'
        },
        mutate: {
            errorPolicy: 'all'
        }
    }
})