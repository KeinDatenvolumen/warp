import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {ApolloProvider} from '@apollo/client/react'
import {BrowserRouter} from 'react-router'
import App from './App'
import '../styles/variables.css'
import '../styles/reset.css'
import '../styles/typography.css'
import '../styles/global.css'
import {apolloClient} from '../api/apolloClient'

const rootElement = document.getElementById('root')

if (!rootElement) {
    throw new Error('Das Root-Element wurde nicht gefunden.')
}

createRoot(rootElement).render(
    <StrictMode>
        <ApolloProvider client={apolloClient}>
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </ApolloProvider>
    </StrictMode>
)