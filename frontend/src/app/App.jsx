import {AuthProvider} from '../features/auth/AuthContext.jsx'
import AppRoutes from './routes.jsx'

export default function App() {
    return (
        <AuthProvider>
            <AppRoutes />
        </AuthProvider>
    )
}