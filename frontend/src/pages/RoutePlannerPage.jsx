import RouteMap from '../components/RouteMap/RouteMap.jsx'
import styles from './PagePlaceholder.module.css'

export default function RoutePlannerPage() {
    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Routenplanung</p>
                <h1>Routenplaner</h1>
                <p className={styles.description}>
                    Hier entsteht später die wetterabhängige Routenplanung.
                </p>
            </section>

            <div style={{marginTop: '1.5rem'}}>
                <RouteMap />
            </div>
        </main>
    )
}