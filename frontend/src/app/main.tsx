import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {ApolloProvider} from '@apollo/client/react'
import {BrowserRouter} from 'react-router'
import {ThemeProvider} from '../features/theme/ThemeContext'
import App from './App'
import '../styles/variables.css'
import '../styles/themes.css'
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
        <ThemeProvider>
            <ApolloProvider client={apolloClient}>
                <BrowserRouter>
                    <App />
                </BrowserRouter>
            </ApolloProvider>
        </ThemeProvider>
    </StrictMode>
)