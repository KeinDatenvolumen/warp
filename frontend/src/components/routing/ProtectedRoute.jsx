import {Navigate, useLocation} from 'react-router'
import {useAuth} from '../../features/auth/useAuth.js'

export default function ProtectedRoute({ children }) {
    const { isAuthenticated } = useAuth()
    const location = useLocation()

    if (!isAuthenticated) {
        return (
            <Navigate
                to="/login"
                replace
                state={{ from: location }}
            />
        )
    }

    return children
}