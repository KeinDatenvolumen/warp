import {useState} from 'react'
import {NavLink, Outlet, useNavigate} from 'react-router'
import ConfirmDialog from '../components/ui/ConfirmDialog/ConfirmDialog'
import {LOGOUT_REDIRECT_PATH} from '../features/auth/authRedirect'
import {useAuth} from '../features/auth/useAuth'
import styles from './AppLayout.module.css'
import type {NavLinkRenderProps} from 'react-router'

function navigationClassName({
                                 isActive
                             }: NavLinkRenderProps): string {
    return [
        styles.navigationLink,
        isActive ? styles.navigationLinkActive : null
    ]
        .filter(Boolean)
        .join(' ')
}

export default function AppLayout() {
    const {status, isAuthenticated, logout} = useAuth()
    const navigate = useNavigate()
    const [isLoggingOut, setIsLoggingOut] = useState(false)
    const [logoutError, setLogoutError] = useState<string>()
    const [isConfirmOpen, setIsConfirmOpen] = useState(false)

    async function handleLogout(): Promise<void> {
        setIsConfirmOpen(false)

        if (isLoggingOut) {
            return
        }

        setIsLoggingOut(true)
        setLogoutError(undefined)

        try {
            await logout()
            navigate(LOGOUT_REDIRECT_PATH, {replace: true})
        } catch {
            setLogoutError(
                'Die Abmeldung konnte nicht abgeschlossen werden. Bitte versuche es erneut.'
            )
        } finally {
            setIsLoggingOut(false)
        }
    }

    return (
        <div className={styles.shell}>
            <header className={styles.header}>
                <div className={styles.headerContent}>
                    <NavLink to="/" className={styles.brand} end>
                        <span className={styles.brandMark}>W</span>
                        <span className={styles.brandText}>
                            <strong>WaRP</strong>
                            <small>Weather-aware Route Planning</small>
                        </span>
                    </NavLink>
                    <nav className={styles.navigation} aria-label="Hauptnavigation">
                        <NavLink to="/" className={navigationClassName} end>Startseite</NavLink>
                        <NavLink to="/system-status" className={navigationClassName}>Systemstatus</NavLink>
                        <NavLink to="/route-planner" className={navigationClassName}>Routenplaner</NavLink>
                        {isAuthenticated && <>
                            <NavLink to="/profile" className={navigationClassName}>Profil</NavLink>
                            <NavLink to="/saved-routes" className={navigationClassName}>Gespeicherte Routen</NavLink>
                        </>}
                        {status === 'unauthenticated' && (
                            <NavLink to="/login" className={navigationClassName}>Anmelden</NavLink>
                        )}
                        {isAuthenticated && (
                            <button
                                type="button"
                                className={`${styles.navigationLink} ${styles.navigationButton} ${styles.logoutButton}`}
                                onClick={() => setIsConfirmOpen(true)}
                                disabled={isLoggingOut}
                                aria-busy={isLoggingOut}
                            >
                                {isLoggingOut ? 'Abmeldung läuft …' : 'Abmelden'}
                            </button>
                        )}
                    </nav>
                </div>
            </header>
            {logoutError && (
                <p className={styles.logoutError} role="alert">
                    {logoutError}
                </p>
            )}
            <div className={styles.content}><Outlet/></div>
            <ConfirmDialog
                open={isConfirmOpen}
                title="Wirklich abmelden?"
                description="Du musst dich danach erneut anmelden, um Routen zu planen und zu speichern."
                confirmLabel="Abmelden"
                onConfirm={handleLogout}
                onCancel={() => setIsConfirmOpen(false)}
            />
            <footer className={styles.footer}>
                <div className={styles.footerContent}><span>WaRP</span><span>Weather-aware Route Planning</span></div>
            </footer>
        </div>
    )
}