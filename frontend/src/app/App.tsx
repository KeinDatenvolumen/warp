import {AuthProvider} from '../features/auth/AuthContext'
import AppRoutes from './routes'

export default function App() {
    return (
        <AuthProvider>
            <AppRoutes />
        </AuthProvider>
    )
}