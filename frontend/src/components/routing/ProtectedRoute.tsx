// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-126] (8495425) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: manage session status centrally and implement login form (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
import {Navigate, useLocation} from 'react-router'
import type {PropsWithChildren} from 'react'
import Loader from '../ui/Loader/Loader'
import MessageCard from '../ui/MessageCard/MessageCard'
import {LOGIN_PATH} from '../../features/auth/authRedirect'
import {useAuth} from '../../features/auth/useAuth'
import styles from '../../pages/PagePlaceholder.module.css'

interface ProtectedRouteProps extends PropsWithChildren {
    requireVerified?: boolean
}

export default function ProtectedRoute({
                                           children,
                                           requireVerified = false
                                       }: ProtectedRouteProps) {
    const {status, session} = useAuth()
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

    if (requireVerified && session?.emailVerified === false) {
        return <Navigate to="/route-planner" replace />
    }

    return children
}