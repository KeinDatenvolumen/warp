import { useCallback, useEffect, useState } from 'react'

const REFRESH_INTERVAL_MS = 5000

const BFF_BASE_URL = (
    import.meta.env.VITE_BFF_BASE_URL ?? ''
).replace(/\/$/, '')

function SystemHealthDashboard() {
    const [health, setHealth] = useState(null)
    const [loading, setLoading] = useState(true)
    const [refreshing, setRefreshing] = useState(false)
    const [error, setError] = useState(null)

    const loadHealth = useCallback(async (manualRefresh = false) => {
        if (manualRefresh) {
            setRefreshing(true)
        }

        try {
            const response = await fetch(`${BFF_BASE_URL}/api/v1/system/health`, {
                method: 'GET',
                headers: {
                    Accept: 'application/json'
                },
                cache: 'no-store'
            })

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`)
            }

            const data = await response.json()

            setHealth(data)
            setError(null)
        } catch {
            setError(
                'Der BFF-Service ist nicht erreichbar. ' +
                'Prüfe, ob der BFF gestartet und erreichbar ist.'
            )
        } finally {
            setLoading(false)
            setRefreshing(false)
        }
    }, [])

    useEffect(() => {
        loadHealth()

        const intervalId = window.setInterval(
            () => loadHealth(),
            REFRESH_INTERVAL_MS
        )

        return () => window.clearInterval(intervalId)
    }, [loadHealth])

    const allHealthy =
        health !== null &&
        health.healthyServices === health.totalServices

    return (
        <main className="page">
            <header className="hero">
                <div>
                    <p className="eyebrow">Weather-aware Route Planning</p>
                    <h1>WaRP Service Status</h1>
                    <p className="subtitle">
                        Live-Übersicht der lokalen Microservices
                    </p>
                </div>

                <button
                    className="refresh-button"
                    type="button"
                    disabled={refreshing}
                    onClick={() => loadHealth(true)}
                >
                    {refreshing ? 'Aktualisiere...' : 'Jetzt aktualisieren'}
                </button>
            </header>

            {loading && (
                <section className="message-card">
                    <span className="loader" aria-hidden="true" />
                    <p>Servicezustände werden geladen...</p>
                </section>
            )}

            {!loading && error && (
                <section className="message-card error-card" role="alert">
                    <div className="large-status-dot unavailable" />
                    <div>
                        <h2>BFF nicht erreichbar</h2>
                        <p>{error}</p>
                    </div>
                </section>
            )}

            {!loading && health && (
                <>
                    <section
                        className={
                            allHealthy
                                ? 'summary healthy-summary'
                                : 'summary degraded-summary'
                        }
                    >
                        <div>
                            <p className="summary-label">Gesamtstatus</p>
                            <h2>
                                {allHealthy
                                    ? 'Alle Systeme betriebsbereit'
                                    : 'Mindestens ein Service ist gestört'}
                            </h2>
                        </div>

                        <div className="summary-count">
                            <strong>{health.healthyServices}</strong>
                            <span>von {health.totalServices} erreichbar</span>
                        </div>
                    </section>

                    <section className="service-grid">
                        {health.services.map((service) => (
                            <ServiceCard
                                key={service.service}
                                service={service}
                            />
                        ))}
                    </section>

                    <footer className="dashboard-footer">
            <span>
              Automatische Aktualisierung alle fünf Sekunden
            </span>
                        <span>
              Letzte Prüfung: {formatTimestamp(health.checkedAt)}
            </span>
                    </footer>
                </>
            )}
        </main>
    )
}

function ServiceCard({ service }) {
    const healthy =
        service.reachable && service.status === 'UP'

    return (
        <article
            className={
                healthy
                    ? 'service-card service-up'
                    : 'service-card service-down'
            }
        >
            <div className="service-card-header">
                <div
                    className={
                        healthy
                            ? 'status-dot up'
                            : 'status-dot unavailable'
                    }
                    aria-hidden="true"
                />

                <span
                    className={
                        healthy
                            ? 'status-badge badge-up'
                            : 'status-badge badge-down'
                    }
                >
          {healthy ? 'Healthy' : service.status}
        </span>
            </div>

            <h2>{formatServiceName(service.service)}</h2>

            <dl className="service-details">
                <div>
                    <dt>Erreichbarkeit</dt>
                    <dd>{service.reachable ? 'Erreichbar' : 'Nicht erreichbar'}</dd>
                </div>

                <div>
                    <dt>Antwortzeit</dt>
                    <dd>{service.responseTimeMs} ms</dd>
                </div>
            </dl>
        </article>
    )
}

function formatServiceName(serviceName) {
    return serviceName
        .split('-')
        .map((part) => (
            part.charAt(0).toUpperCase() + part.slice(1)
        ))
        .join(' ')
}

function formatTimestamp(timestamp) {
    if (!timestamp) {
        return 'unbekannt'
    }

    return new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'medium'
    }).format(new Date(timestamp))
}

export default SystemHealthDashboard