// [KI-Log Nr. KI-04] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): GraphQL BFF & Apollo Client (WARP-003/004)
import {Link, useLocation} from 'react-router'
import styles from './NotFoundPage.module.css'

export function NotFoundPage() {
    const location = useLocation();
    return <main className={styles.page}>
        <section className={styles.card}><p className={styles.code}>404</p><p className={styles.eyebrow}>Seite nicht
            gefunden</p><h1>Diese Route existiert nicht</h1><p className={styles.description}>Für den
            Pfad <code>{location.pathname}</code> wurde keine Seite gefunden.</p>
            <div className={styles.actions}><Link to="/" className={styles.primaryLink}>Zur Startseite</Link><Link
                to="/system-status" className={styles.secondaryLink}>Systemstatus öffnen</Link></div>
        </section>
    </main>
}
