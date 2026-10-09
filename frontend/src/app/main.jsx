import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ApolloProvider } from '@apollo/client/react'
import { BrowserRouter} from "react-router";
import App from './App.jsx'
import '../layout/styles.css'
import { apolloClient } from './api/apolloClient.js'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <ApolloProvider client={apolloClient}>
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </ApolloProvider>
    </StrictMode>
)