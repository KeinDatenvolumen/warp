import {Navigate, useLocation} from 'react-router'
import type {PropsWithChildren} from 'react'
import Loader from '../ui/Loader/Loader'
import MessageCard from '../ui/MessageCard/MessageCard'
import {LOGIN_PATH} from '../../features/auth/authRedirect'
import {useAuth} from '../../features/auth/useAuth'
import styles from '../../pages/PagePlaceholder.module.css'

export default function ProtectedRoute({
                                           children
                                       }: PropsWithChildren) {
    const {status} = useAuth()
    const location = useLocation()
    if (status === 'loading') {
        return (
            <main className={styles.page}>
                <MessageCard>
                    <Loader/>
                    <p role="status">Sitzung wird geprüft …</p>
                </MessageCard>
            </main>
        )
    }

    if (status === 'unauthenticated') {
        return (
            <Navigate
                to={LOGIN_PATH}
                replace
                state={{from: location}}
            />
        )
    }

    return children
}