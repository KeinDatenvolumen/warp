import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ApolloProvider } from '@apollo/client/react'
import { BrowserRouter} from "react-router";
import App from './App.jsx'
import { apolloClient } from './api/apolloClient.js'
import './styles.css'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <ApolloProvider client={apolloClient}>
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </ApolloProvider>
    </StrictMode>
)