import {Navigate, useLocation} from 'react-router'
import type {PropsWithChildren} from 'react'
import {useAuth} from '../../features/auth/useAuth'

export default function ProtectedRoute({
                                           children
                                       }: PropsWithChildren) {
    const {isAuthenticated} = useAuth()
    const location = useLocation()

    if (!isAuthenticated) {
        return (
            <Navigate
                to="/login"
                replace
                state={{from: location}}
            />
        )
    }

    return children
}