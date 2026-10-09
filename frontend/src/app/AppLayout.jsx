import {NavLink, Outlet} from 'react-router'
import {useAuth} from '../features/auth/useAuth.js'
import styles from './AppLayout.module.css'

function navigationClassName({ isActive }) {
    return [styles.navigationLink, isActive ? styles.navigationLinkActive : null]
        .filter(Boolean).join(' ')
}

export default function AppLayout() {
    const {isAuthenticated, logout} = useAuth()
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
                        {!isAuthenticated && <NavLink to="/login" className={navigationClassName}>Anmelden</NavLink>}
                        {isAuthenticated &&
                            <button type="button" className={`${styles.navigationLink} ${styles.navigationButton}`}
                                    onClick={logout}>Abmelden</button>}
                    </nav>
                </div>
            </header>
            <div className={styles.content}><Outlet/></div>
            <footer className={styles.footer}>
                <div className={styles.footerContent}><span>WaRP</span><span>Weather-aware Route Planning</span></div>
            </footer>
        </div>
    )
}
