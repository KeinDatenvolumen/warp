import { Link } from 'react-router'

export function HomePage() {
    return (
        <main className="page">
            <section className="welcome-card">
                <p className="eyebrow">
                    Weather-aware Route Planning
                </p>

                <h1>Willkommen bei WaRP</h1>

                <p className="welcome-description">
                    WaRP verbindet Routenplanung mit zeitlich und räumlich
                    passenden Wetterinformationen.
                </p>

                <div className="welcome-actions">
                    <Link
                        to="/system-status"
                        className="primary-link"
                    >
                        Systemstatus öffnen
                    </Link>
                </div>
            </section>

            <section className="feature-grid">
                <article className="feature-card">
                    <span className="feature-number">01</span>
                    <h2>Route planen</h2>
                    <p>
                        Routen für Fahrrad, Motorrad und Wandern
                        wetterabhängig vorbereiten.
                    </p>
                    <span className="feature-state">
                        In Vorbereitung
                    </span>
                </article>

                <article className="feature-card">
                    <span className="feature-number">02</span>
                    <h2>Wetter bewerten</h2>
                    <p>
                        Wetterprognosen passend zum zeitlichen Verlauf
                        einer Route auswerten.
                    </p>
                    <span className="feature-state">
                        In Vorbereitung
                    </span>
                </article>

                <article className="feature-card">
                    <span className="feature-number">03</span>
                    <h2>System überwachen</h2>
                    <p>
                        Erreichbarkeit und Antwortzeit aller
                        WaRP-Services kontrollieren.
                    </p>
                    <Link
                        to="/system-status"
                        className="feature-link"
                    >
                        Status anzeigen
                    </Link>
                </article>
            </section>
        </main>
    )
}