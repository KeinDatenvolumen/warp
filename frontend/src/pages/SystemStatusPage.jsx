import { useQuery } from '@apollo/client/react'
import { SYSTEM_HEALTH_QUERY } from '../api/systemHealthQuery.js'

function formatCheckedAt(value) {
    if (!value) {
        return 'Unbekannt'
    }

    return new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'medium',
    }).format(new Date(value))
}

function formatResponseTime(responseTimeMs) {
    if (responseTimeMs === null || responseTimeMs === undefined) {
        return 'Nicht verfügbar'
    }

    return `${responseTimeMs} ms`
}

function ServiceCard({ service }) {
    const isAvailable = service.reachable && service.status === 'UP'

    return (
        <article
            className={
                isAvailable
                    ? 'service-card service-up'
                    : 'service-card service-down'
            }
        >
            <div className="service-card-header">
                <span
                    className={
                        isAvailable
                            ? 'status-dot up'
                            : 'status-dot unavailable'
                    }
                    aria-hidden="true"
                />

                <span
                    className={
                        isAvailable
                            ? 'status-badge badge-up'
                            : 'status-badge badge-down'
                    }
                >
                    {service.status}
                </span>
            </div>

            <h2>{service.service}</h2>

            <dl className="service-details">
                <div>
                    <dt>Erreichbar</dt>
                    <dd>{service.reachable ? 'Ja' : 'Nein'}</dd>
                </div>

                <div>
                    <dt>Antwortzeit</dt>
                    <dd>
                        {formatResponseTime(service.responseTimeMs)}
                    </dd>
                </div>
            </dl>
        </article>
    )
}

export function SystemStatusPage() {
    const {
        data,
        loading,
        error,
        refetch,
        networkStatus,
    } = useQuery(SYSTEM_HEALTH_QUERY, {
        fetchPolicy: 'network-only',
        notifyOnNetworkStatusChange: true,
    })

    const isRefreshing = networkStatus === 4

    if (loading && !data) {
        return (
            <main className="page">
                <section className="message-card">
                    <span
                        className="loader"
                        aria-hidden="true"
                    />

                    <div>
                        <h1>Systemstatus wird geladen</h1>
                        <p>
                            Die Statusinformationen werden über GraphQL
                            vom BFF abgerufen.
                        </p>
                    </div>
                </section>
            </main>
        )
    }

    if (error && !data) {
        return (
            <main className="page">
                <section className="message-card error-card">
                    <span
                        className="large-status-dot unavailable"
                        aria-hidden="true"
                    />

                    <div>
                        <h1>Systemstatus nicht verfügbar</h1>
                        <p>{error.message}</p>

                        <button
                            type="button"
                            className="refresh-button message-action"
                            onClick={() => refetch()}
                        >
                            Erneut versuchen
                        </button>
                    </div>
                </section>
            </main>
        )
    }

    const systemHealth = data?.systemHealth

    if (!systemHealth) {
        return (
            <main className="page">
                <section className="message-card error-card">
                    <span
                        className="large-status-dot unavailable"
                        aria-hidden="true"
                    />

                    <div>
                        <h1>Keine Statusdaten erhalten</h1>
                        <p>
                            Der BFF hat keine Systemstatusdaten geliefert.
                        </p>

                        <button
                            type="button"
                            className="refresh-button message-action"
                            onClick={() => refetch()}
                        >
                            Erneut versuchen
                        </button>
                    </div>
                </section>
            </main>
        )
    }

    const isHealthy = systemHealth.status === 'UP'

    return (
        <main className="page">
            <header className="hero">
                <div>
                    <p className="eyebrow">
                        GraphQL-Serviceüberwachung
                    </p>

                    <h1>Systemstatus</h1>

                    <p className="subtitle">
                        Aktueller Zustand des BFF und der internen
                        WaRP-Services.
                    </p>
                </div>

                <button
                    type="button"
                    className="refresh-button"
                    onClick={() => refetch()}
                    disabled={isRefreshing}
                >
                    {isRefreshing
                        ? 'Wird aktualisiert ...'
                        : 'Status aktualisieren'}
                </button>
            </header>

            <section
                className={
                    isHealthy
                        ? 'summary healthy-summary'
                        : 'summary degraded-summary'
                }
            >
                <div>
                    <p className="summary-label">
                        Gesamtstatus
                    </p>

                    <h2>
                        {isHealthy
                            ? 'Alle Systeme sind verfügbar'
                            : 'Mindestens ein System ist eingeschränkt'}
                    </h2>
                </div>

                <div className="summary-count">
                    <strong>
                        {systemHealth.healthyServices}/
                        {systemHealth.totalServices}
                    </strong>
                    <span>Services erreichbar</span>
                </div>
            </section>

            <section
                className="service-grid"
                aria-label="Status der WaRP-Services"
            >
                {systemHealth.services.map((service) => (
                    <ServiceCard
                        key={service.service}
                        service={service}
                    />
                ))}
            </section>

            <footer className="dashboard-footer">
                <span>
                    GraphQL-Verbindung erfolgreich
                </span>

                <span>
                    Zuletzt geprüft:{' '}
                    {formatCheckedAt(systemHealth.checkedAt)}
                </span>
            </footer>
        </main>
    )
}