import {useCallback, useEffect, useState} from 'react'
import ServiceCard from '../../components/ui/ServiceCard/ServiceCard'
import Loader from '../../components/ui/Loader/Loader'
import MessageCard from '../../components/ui/MessageCard/MessageCard'
import styles from './SystemHealthDashboard.module.css'
import type {SystemHealth} from '../../types/systemHealth'

const REFRESH_INTERVAL_MS = 5000
const BFF_BASE_URL = (import.meta.env.VITE_BFF_BASE_URL ?? '').replace(/\/$/, '')
export default function SystemHealthDashboard() {
    const [health, setHealth] = useState<SystemHealth | null>(null)
    const [loading, setLoading] = useState(true)
    const [refreshing, setRefreshing] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const loadHealth = useCallback(async (
        manual = false
    ): Promise<void> => {
        if (manual) setRefreshing(true);
        try {
            const response = await fetch(`${BFF_BASE_URL}/api/v1/system/health`, {
                headers: {Accept: 'application/json'},
                cache: 'no-store'
            });
            if (!response.ok) throw new Error();
            const result = await response.json() as SystemHealth
            setHealth(result)
            setError(null)
        } catch {
            setError('Der BFF-Service ist nicht erreichbar. Prüfe, ob der BFF gestartet und erreichbar ist.')
        } finally {
            setLoading(false);
            setRefreshing(false)
        }
    }, [])
    useEffect(() => {
        loadHealth();
        const id = window.setInterval(() => loadHealth(), REFRESH_INTERVAL_MS);
        return () => window.clearInterval(id)
    }, [loadHealth])
    const allHealthy = health && health.healthyServices === health.totalServices
    return <main className={styles.page}>
        <header className={styles.hero}>
            <div><p className={styles.eyebrow}>Weather-aware Route Planning</p><h1>WaRP Service Status</h1><p
                className={styles.subtitle}>Live-Übersicht der lokalen Microservices</p></div>
            <button className={styles.button} type="button" disabled={refreshing}
                    onClick={() => loadHealth(true)}>{refreshing ? 'Aktualisiere...' : 'Jetzt aktualisieren'}</button>
        </header>
        {loading && <MessageCard><Loader/><p>Servicezustände werden geladen...</p></MessageCard>}
        {!loading && error && <MessageCard error><span className={styles.largeErrorDot}/>
            <div><h2>BFF nicht erreichbar</h2><p>{error}</p></div>
        </MessageCard>}
        {!loading && health && <>
            <section className={`${styles.summary} ${allHealthy ? styles.healthySummary : styles.degradedSummary}`}>
                <div><p className={styles.summaryLabel}>Gesamtstatus</p>
                    <h2>{allHealthy ? 'Alle Systeme betriebsbereit' : 'Mindestens ein Service ist gestört'}</h2></div>
                <div className={styles.summaryCount}>
                    <strong>{health.healthyServices}</strong><span>von {health.totalServices} erreichbar</span></div>
            </section>
            <section className={styles.grid}>{health.services.map(service => <ServiceCard key={service.service}
                                                                                          service={service}
                                                                                          formatName={name => name.split('-').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join(' ')}/>)}</section>
            <footer className={styles.footer}><span>Automatische Aktualisierung alle fünf Sekunden</span><span>Letzte Prüfung: {health.checkedAt ? new Intl.DateTimeFormat('de-DE', {
                dateStyle: 'medium',
                timeStyle: 'medium'
            }).format(new Date(health.checkedAt)) : 'unbekannt'}</span></footer>
        </>}</main>
}
