import { AuthProvider } from '../features/auth/authContext.jsx'
import AppRoutes from './routes.jsx'

export default function App() {
    return (
        <AuthProvider>
            <AppRoutes />
        </AuthProvider>
    )
}