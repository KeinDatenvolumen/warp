import { BrowserRouter } from 'react-router'
import AppRoutes from './routes.jsx'

export default function App() {
    return (
        <BrowserRouter>
            <AppRoutes />
        </BrowserRouter>
    )
}