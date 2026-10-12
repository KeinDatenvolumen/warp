import {useQuery} from '@apollo/client/react'
import {SYSTEM_HEALTH_QUERY} from '../api/systemHealthQuery'
import ServiceCard from '../components/ui/ServiceCard/ServiceCard'
import Loader from '../components/ui/Loader/Loader'
import MessageCard from '../components/ui/MessageCard/MessageCard'
import styles from './SystemStatusPage.module.css'

function formatCheckedAt(value: string | null): string {
    return value
        ? new Intl.DateTimeFormat('de-DE', {
            dateStyle: 'medium',
            timeStyle: 'medium'
        }).format(new Date(value))
        : 'Unbekannt'
}

export function SystemStatusPage() {
    const {data, loading, error, refetch, networkStatus} = useQuery(SYSTEM_HEALTH_QUERY, {
        fetchPolicy: 'network-only',
        errorPolicy: 'all',
        notifyOnNetworkStatusChange: true
    })
    const isRefreshing = networkStatus === 4
    if (loading && !data) return <main className={styles.page}><MessageCard><Loader/>
        <div><h1>Systemstatus wird geladen</h1><p>Die Statusinformationen werden über GraphQL vom BFF abgerufen.</p>
        </div>
    </MessageCard></main>
    if (error && !data) return <main className={styles.page}><MessageCard error><span className={styles.largeErrorDot}
                                                                                      aria-hidden="true"/>
        <div><h1>Systemstatus nicht verfügbar</h1><p>{error.message}</p>
            <button type="button" className={`${styles.button} ${styles.messageAction}`}
                    onClick={() => refetch()}>Erneut versuchen
            </button>
        </div>
    </MessageCard></main>
    const systemHealth = data?.systemHealth
    if (!systemHealth) return <main className={styles.page}><MessageCard error><span className={styles.largeErrorDot}
                                                                                     aria-hidden="true"/>
        <div><h1>Keine Statusdaten erhalten</h1><p>Der BFF hat keine Systemstatusdaten geliefert.</p>
            <button type="button" className={`${styles.button} ${styles.messageAction}`}
                    onClick={() => refetch()}>Erneut versuchen
            </button>
        </div>
    </MessageCard></main>
    const isHealthy = systemHealth.status === 'UP'
    return <main className={styles.page}>
        <header className={styles.hero}>
            <div><p className={styles.eyebrow}>GraphQL-Serviceüberwachung</p><h1>Systemstatus</h1><p
                className={styles.subtitle}>Aktueller Zustand des BFF und der internen WaRP-Services.</p></div>
            <button type="button" className={styles.button} onClick={() => refetch()}
                    disabled={isRefreshing}>{isRefreshing ? 'Wird aktualisiert ...' : 'Status aktualisieren'}</button>
        </header>
        <section className={`${styles.summary} ${isHealthy ? styles.healthySummary : styles.degradedSummary}`}>
            <div><p className={styles.summaryLabel}>Gesamtstatus</p>
                <h2>{isHealthy ? 'Alle Systeme sind verfügbar' : 'Mindestens ein System ist eingeschränkt'}</h2></div>
            <div className={styles.summaryCount}>
                <strong>{systemHealth.healthyServices}/{systemHealth.totalServices}</strong><span>Services erreichbar</span>
            </div>
        </section>
        <section className={styles.grid} aria-label="Status der WaRP-Services">{systemHealth.services.map(service =>
            <ServiceCard key={service.service} service={service}/>)}</section>
        <footer className={styles.dashboardFooter}>
            <span>GraphQL-Verbindung erfolgreich</span><span>Zuletzt geprüft: {formatCheckedAt(systemHealth.checkedAt)}</span>
        </footer>
    </main>
}
