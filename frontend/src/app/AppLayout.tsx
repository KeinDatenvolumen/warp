// [KI-Log Nr. KI-032] (783455d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add application pages and routing (WARP-004)
// [KI-Log Nr. KI-039] (b0345ea) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add application pages and routing (WARP-004)
// [KI-Log Nr. KI-041] (95411c2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: rebase to live (WARP-004)
// [KI-Log Nr. KI-046] (90625b3) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add global application navigation (WARP-004)
// [KI-Log Nr. KI-047] (a05e6ff) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: only renders navigation tabs conditionally (WARP-004)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-087] (486dab2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-090] (52b5c38) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: registration error handling and design (WARP-005)
// [KI-Log Nr. KI-109] (de61707) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-111] (2b00488) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: registration error handling and design (WARP-005)
// [KI-Log Nr. KI-127] (2b97154) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add logout action with loading state, error handling and redirect (WARP-006)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-211] (d6d8b5b) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix(WARP-031): refine default theme and navigation design
// [KI-Log Nr. KI-224] (b31c3e8) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix(WARP-031): fix navigation layering and dark mode contrast
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
    const {status, isAuthenticated, session, logout} = useAuth()
    const isEmailVerified = session?.emailVerified ?? true
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
                                    {isEmailVerified && (
                                        <NavLink
                                            to="/saved-routes"
                                            className={navigationClassName}
                                        >
                                            Gespeicherte Routen
                                        </NavLink>
                                    )}

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