import { Link, useLocation } from 'react-router'

export function NotFoundPage() {
    const location = useLocation()

    return (
        <main className="page">
            <section className="not-found-card">
                <p className="not-found-code">404</p>

                <p className="eyebrow">
                    Seite nicht gefunden
                </p>

                <h1>Diese Route existiert nicht</h1>

                <p className="not-found-description">
                    Für den Pfad{' '}
                    <code>{location.pathname}</code>{' '}
                    wurde keine Seite gefunden.
                </p>

                <div className="not-found-actions">
                    <Link
                        to="/"
                        className="primary-link"
                    >
                        Zur Startseite
                    </Link>

                    <Link
                        to="/system-status"
                        className="secondary-link"
                    >
                        Systemstatus öffnen
                    </Link>
                </div>
            </section>
        </main>
    )
}