// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): UI-Redesign & Responsive Navigation (WARP-031)
import {useEffect, useRef, useState} from 'react'
import {
    NavLink,
    Outlet,
    useLocation,
    useNavigate
} from 'react-router'
import type {NavLinkRenderProps} from 'react-router'
import ConfirmDialog from '../components/ui/ConfirmDialog/ConfirmDialog'
import {LOGOUT_REDIRECT_PATH} from '../features/auth/authRedirect'
import {useAuth} from '../features/auth/useAuth'
import styles from './AppLayout.module.css'

function navigationClassName({
                                 isActive
                             }: NavLinkRenderProps): string {
    return [
        styles.navigationLink,
        isActive ? styles.navigationLinkActive : ''
    ]
        .filter(Boolean)
        .join(' ')
}

export default function AppLayout() {
    const {status, isAuthenticated, logout} = useAuth()
    const navigate = useNavigate()
    const location = useLocation()

    const [isLoggingOut, setIsLoggingOut] = useState(false)
    const [logoutError, setLogoutError] = useState<string>()
    const [isConfirmOpen, setIsConfirmOpen] = useState(false)
    const [isNavigationOpen, setIsNavigationOpen] = useState(false)

    const menuButtonRef = useRef<HTMLButtonElement>(null)
    const navigationPanelRef = useRef<HTMLDivElement>(null)
    const navigationWasOpenRef = useRef(false)

    useEffect(() => {
        setIsNavigationOpen(false)
    }, [location.pathname])

    useEffect(() => {
        const navigationWasOpen = navigationWasOpenRef.current
        navigationWasOpenRef.current = isNavigationOpen

        if (!isNavigationOpen) {
            document.body.style.removeProperty('overflow')

            if (navigationWasOpen) {
                menuButtonRef.current?.focus()
            }

            return
        }

        document.body.style.overflow = 'hidden'

        const navigationPanel = navigationPanelRef.current

        if (!navigationPanel) {
            return
        }

        const selector = [
            'a[href]',
            'button:not([disabled])',
            'input:not([disabled])',
            'select:not([disabled])',
            'textarea:not([disabled])',
            '[tabindex]:not([tabindex="-1"])'
        ].join(',')

        const focusableElements = Array.from(
            navigationPanel.querySelectorAll<HTMLElement>(selector)
        )

        focusableElements[0]?.focus()

        function handleKeyDown(event: KeyboardEvent): void {
            if (event.key === 'Escape') {
                event.preventDefault()
                setIsNavigationOpen(false)
                return
            }

            if (
                event.key !== 'Tab' ||
                focusableElements.length === 0
            ) {
                return
            }

            const firstElement = focusableElements[0]
            const lastElement =
                focusableElements[focusableElements.length - 1]

            if (!firstElement || !lastElement) {
                return
            }

            if (
                event.shiftKey &&
                document.activeElement === firstElement
            ) {
                event.preventDefault()
                lastElement.focus()
                return
            }

            if (
                !event.shiftKey &&
                document.activeElement === lastElement
            ) {
                event.preventDefault()
                firstElement.focus()
            }
        }

        document.addEventListener('keydown', handleKeyDown)

        return () => {
            document.body.style.removeProperty('overflow')
            document.removeEventListener('keydown', handleKeyDown)
        }
    }, [isNavigationOpen])

    async function handleLogout(): Promise<void> {
        setIsConfirmOpen(false)
        setIsNavigationOpen(false)

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

    function openLogoutDialog(): void {
        setIsNavigationOpen(false)
        setIsConfirmOpen(true)
    }

    return (
        <div className={styles.shell}>
            <header className={styles.header}>
                <div className={styles.headerContent}>
                    <NavLink
                        to="/route-planner"
                        className={styles.brand}
                        aria-label="WaRP Routenplaner"
                    >
    <span
        className={styles.brandMark}
        aria-hidden="true"
    >
        W
    </span>

                        <span className={styles.brandText}>
        <strong>WaRP</strong>

        <small>
            Weather-aware Route Planning
        </small>
    </span>
                    </NavLink>

                    <button
                        ref={menuButtonRef}
                        type="button"
                        className={styles.menuButton}
                        aria-label={
                            isNavigationOpen
                                ? 'Navigation schließen'
                                : 'Navigation öffnen'
                        }
                        aria-expanded={isNavigationOpen}
                        aria-controls="main-navigation-panel"
                        onClick={() => {
                            setIsNavigationOpen(
                                (currentValue) => !currentValue
                            )
                        }}
                    >
                        <span
                            className={styles.menuIcon}
                            aria-hidden="true"
                        >
                            <span></span>
                            <span></span>
                            <span></span>
                        </span>
                    </button>

                    <div
                        className={[
                            styles.navigationOverlay,
                            isNavigationOpen
                                ? styles.navigationOverlayOpen
                                : ''
                        ]
                            .filter(Boolean)
                            .join(' ')}
                        aria-hidden="true"
                        onClick={() => {
                            setIsNavigationOpen(false)
                        }}
                    ></div>

                    <div
                        ref={navigationPanelRef}
                        id="main-navigation-panel"
                        className={[
                            styles.navigationPanel,
                            isNavigationOpen
                                ? styles.navigationPanelOpen
                                : ''
                        ]
                            .filter(Boolean)
                            .join(' ')}
                    >
                        <div className={styles.mobileNavigationHeader}>
                            <span
                                className={styles.mobileNavigationTitle}
                            >
                                Navigation
                            </span>

                            <button
                                type="button"
                                className={styles.closeButton}
                                aria-label="Navigation schließen"
                                onClick={() => {
                                    setIsNavigationOpen(false)
                                }}
                            >
                                <span aria-hidden="true">×</span>
                            </button>
                        </div>
                        <nav
                            className={styles.navigation}
                            aria-label="Hauptnavigation"
                        >
                            <NavLink
                                to="/route-planner"
                                className={navigationClassName}
                            >
                                Routenplaner
                            </NavLink>

                            {isAuthenticated && (
                                <>
                                    <NavLink
                                        to="/saved-routes"
                                        className={navigationClassName}
                                    >
                                        Gespeicherte Routen
                                    </NavLink>

                                    <NavLink
                                        to="/profile"
                                        className={navigationClassName}
                                    >
                                        Profil
                                    </NavLink>
                                </>
                            )}

                            {status === 'unauthenticated' && (
                                <NavLink
                                    to="/login"
                                    className={navigationClassName}
                                >
                                    Anmelden
                                </NavLink>
                            )}

                            {isAuthenticated && (
                                <button
                                    type="button"
                                    className={[
                                        styles.navigationLink,
                                        styles.navigationButton,
                                        styles.logoutButton,
                                    ].join(' ')}
                                    disabled={isLoggingOut}
                                    aria-busy={isLoggingOut}
                                    onClick={openLogoutDialog}
                                >
                                    {isLoggingOut
                                        ? 'Abmeldung läuft …'
                                        : 'Abmelden'}
                                </button>
                            )}
                        </nav>
                    </div>
                </div>
            </header>

            {logoutError && (
                <p
                    className={styles.logoutError}
                    role="alert"
                >
                    {logoutError}
                </p>
            )}

            <main className={styles.content}>
                <Outlet />
            </main>

            <footer className={styles.footer}>
                <div className={styles.footerContent}>
                    <span>WaRP</span>
                    <span>Weather-aware Route Planning</span>
                </div>
            </footer>

            <ConfirmDialog
                open={isConfirmOpen}
                title="Wirklich abmelden?"
                description="Du musst dich danach erneut anmelden, um Routen zu planen und zu speichern."
                confirmLabel="Abmelden"
                cancelLabel="Abbrechen"
                icon="⎋"
                onConfirm={handleLogout}
                onCancel={() => {
                    setIsConfirmOpen(false)
                }}
            />
        </div>
    )
}