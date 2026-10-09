import { NavLink, Outlet } from 'react-router'

function navigationClassName({ isActive }) {
    return isActive
        ? 'app-navigation-link app-navigation-link-active'
        : 'app-navigation-link'
}

export default function AppLayout() {
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
                            <small>Weather-aware Route Planning</small>
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