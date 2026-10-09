import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../features/auth/useAuth.js'

function navigationClassName({ isActive }) {
    return isActive
        ? 'app-navigation-link app-navigation-link-active'
        : 'app-navigation-link'
}

export default function AppLayout() {
    const {
        isAuthenticated,
        logout
    } = useAuth()

    return (
        <div className="app-shell">
            <header className="app-header">
                <div className="app-header-content">
                    <NavLink
                        to="/"
                        className="app-brand"
                        end
                    >
                        <span className="app-brand-mark">W</span>

                        <span className="app-brand-text">
                            <strong>WaRP</strong>
                            <small>
                                Weather-aware Route Planning
                            </small>
                        </span>
                    </NavLink>

                    <nav
                        className="app-navigation"
                        aria-label="Hauptnavigation"
                    >
                        <NavLink
                            to="/"
                            className={navigationClassName}
                            end
                        >
                            Startseite
                        </NavLink>

                        <NavLink
                            to="/system-status"
                            className={navigationClassName}
                        >
                            Systemstatus
                        </NavLink>

                        <NavLink
                            to="/route-planner"
                            className={navigationClassName}
                        >
                            Routenplaner
                        </NavLink>

                        {isAuthenticated && (
                            <>
                                <NavLink
                                    to="/profile"
                                    className={navigationClassName}
                                >
                                    Profil
                                </NavLink>

                                <NavLink
                                    to="/saved-routes"
                                    className={navigationClassName}
                                >
                                    Gespeicherte Routen
                                </NavLink>
                            </>
                        )}

                        {!isAuthenticated && (
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
                                className="app-navigation-link app-navigation-button"
                                onClick={logout}
                            >
                                Abmelden
                            </button>
                        )}
                    </nav>
                </div>
            </header>

            <div className="app-content">
                <Outlet />
            </div>

            <footer className="app-footer">
                <div className="app-footer-content">
                    <span>WaRP</span>
                    <span>Weather-aware Route Planning</span>
                </div>
            </footer>
        </div>
    )
}