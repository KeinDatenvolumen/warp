// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-031] (dffcf3f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-038] (8b1a41d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add authentication context and local storage state management (WARP-004)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    useCallback,
    useEffect,
    useRef,
    useState,
} from 'react'
import Loader from '../../components/ui/Loader/Loader'
import MessageCard from '../../components/ui/MessageCard/MessageCard'
import ServiceCard from '../../components/ui/ServiceCard/ServiceCard'
import type {SystemHealth} from '../../types/systemHealth'
import styles from './SystemHealthDashboard.module.css'

const REFRESH_INTERVAL_MS = 5000

const BFF_BASE_URL = (
    import.meta.env.VITE_BFF_BASE_URL ?? ''
).replace(/\/$/, '')

const HEALTH_ENDPOINT = `${BFF_BASE_URL}/api/v1/system/health`

function formatServiceName(name: string): string {
    return name
        .split('-')
        .map((part) => {
            if (!part) {
                return part
            }

            return (
                part.charAt(0).toUpperCase() +
                part.slice(1)
            )
        })
        .join(' ')
}

function formatCheckedAt(value: string | null | undefined): string {
    if (!value) {
        return 'Unbekannt'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return 'Unbekannt'
    }

    return new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'medium',
    }).format(date)
}

export default function SystemHealthDashboard() {
    const [health, setHealth] =
        useState<SystemHealth | null>(null)

    const [loading, setLoading] = useState(true)
    const [refreshing, setRefreshing] = useState(false)

    const [error, setError] =
        useState<string | null>(null)

    const requestRunningRef = useRef(false)
    const mountedRef = useRef(true)

    const loadHealth = useCallback(
        async (manual = false): Promise<void> => {
            if (requestRunningRef.current) {
                return
            }

            requestRunningRef.current = true

            if (manual) {
                setRefreshing(true)
            }

            try {
                const response = await fetch(
                    HEALTH_ENDPOINT,
                    {
                        headers: {
                            Accept: 'application/json',
                        },
                        cache: 'no-store',
                    },
                )

                if (!response.ok) {
                    throw new Error(
                        `Health-Abfrage fehlgeschlagen: ${response.status}`,
                    )
                }

                const result =
                    (await response.json()) as SystemHealth

                if (!mountedRef.current) {
                    return
                }

                setHealth(result)
                setError(null)
            } catch {
                if (!mountedRef.current) {
                    return
                }

                setError(
                    'Der BFF-Service ist nicht erreichbar. Prüfe, ob der BFF gestartet und erreichbar ist.',
                )
            } finally {
                requestRunningRef.current = false

                if (mountedRef.current) {
                    setLoading(false)
                    setRefreshing(false)
                }
            }
        },
        [],
    )

    useEffect(() => {
        mountedRef.current = true

        void loadHealth()

        const intervalId = window.setInterval(() => {
            void loadHealth()
        }, REFRESH_INTERVAL_MS)

        return () => {
            mountedRef.current = false
            window.clearInterval(intervalId)
        }
    }, [loadHealth])

    const allHealthy =
        health !== null &&
        health.healthyServices === health.totalServices

    return (
        <div className={styles.page}>
            <header className={styles.hero}>
                <div className={styles.heroContent}>
                    <p className={styles.eyebrow}>
                        Weather-aware Route Planning
                    </p>

                    <h1>WaRP Service Status</h1>

                    <p className={styles.subtitle}>
                        Live-Übersicht der lokalen Microservices
                        und ihrer Erreichbarkeit.
                    </p>
                </div>

                <button
                    className={styles.button}
                    type="button"
                    disabled={loading || refreshing}
                    aria-busy={refreshing}
                    onClick={() => {
                        void loadHealth(true)
                    }}
                >
                    {refreshing
                        ? 'Aktualisiere …'
                        : 'Jetzt aktualisieren'}
                </button>
            </header>

            {loading && !health && (
                <MessageCard>
                    <Loader />
                    <p role="status">
                        Servicezustände werden geladen …
                    </p>
                </MessageCard>
            )}

            {!loading && error && !health && (
                <MessageCard error>
                    <span
                        className={styles.largeErrorDot}
                        aria-hidden="true"
                    />

                    <div>
                        <h2>BFF nicht erreichbar</h2>
                        <p>{error}</p>
                    </div>
                </MessageCard>
            )}

            {health && (
                <>
                    {error && (
                        <div
                            className={styles.refreshWarning}
                            role="alert"
                        >
                            <span
                                className={
                                    styles.refreshWarningIcon
                                }
                                aria-hidden="true"
                            >
                                !
                            </span>

                            <div>
                                <strong>
                                    Aktualisierung fehlgeschlagen
                                </strong>

                                <p>
                                    Die zuletzt erfolgreich geladenen
                                    Statusdaten bleiben sichtbar.
                                    Der nächste Versuch erfolgt
                                    automatisch.
                                </p>
                            </div>
                        </div>
                    )}

                    <section
                        className={[
                            styles.summary,
                            allHealthy
                                ? styles.healthySummary
                                : styles.degradedSummary,
                        ].join(' ')}
                        aria-labelledby="system-status-title"
                    >
                        <div className={styles.summaryContent}>
                            <p className={styles.summaryLabel}>
                                Gesamtstatus
                            </p>

                            <h2 id="system-status-title">
                                {allHealthy
                                    ? 'Alle Systeme betriebsbereit'
                                    : 'Mindestens ein Service ist gestört'}
                            </h2>

                            <p className={styles.summaryDescription}>
                                {allHealthy
                                    ? 'Alle überwachten Services antworten wie erwartet.'
                                    : 'Prüfe die unten aufgeführten Services auf Fehler oder fehlende Verbindungen.'}
                            </p>
                        </div>

                        <div className={styles.summaryCount}>
                            <strong>
                                {health.healthyServices}
                            </strong>

                            <span>
                                von {health.totalServices}{' '}
                                erreichbar
                            </span>
                        </div>
                    </section>

                    <section
                        className={styles.servicesSection}
                        aria-labelledby="services-title"
                    >
                        <div className={styles.sectionHeading}>
                            <div>
                                <p className={styles.sectionEyebrow}>
                                    Einzelstatus
                                </p>

                                <h2 id="services-title">
                                    Überwachte Services
                                </h2>
                            </div>

                            <span className={styles.serviceCount}>
                                {health.totalServices}{' '}
                                {health.totalServices === 1
                                    ? 'Service'
                                    : 'Services'}
                            </span>
                        </div>

                        {health.services.length > 0 ? (
                            <div className={styles.grid}>
                                {health.services.map(
                                    (service) => (
                                        <ServiceCard
                                            key={service.service}
                                            service={service}
                                            formatName={
                                                formatServiceName
                                            }
                                        />
                                    ),
                                )}
                            </div>
                        ) : (
                            <div className={styles.emptyState}>
                                <h3>
                                    Keine Services vorhanden
                                </h3>

                                <p>
                                    Der BFF hat keine überwachten
                                    Services zurückgegeben.
                                </p>
                            </div>
                        )}
                    </section>

                    <footer className={styles.footer}>
                        <span>
                            Automatische Aktualisierung alle fünf
                            Sekunden
                        </span>

                        <span>
                            Letzte erfolgreiche Prüfung:{' '}
                            <time dateTime={health.checkedAt ?? undefined}>
                                {formatCheckedAt(
                                    health.checkedAt,
                                )}
                            </time>
                        </span>
                    </footer>
                </>
            )}
        </div>
    )
}