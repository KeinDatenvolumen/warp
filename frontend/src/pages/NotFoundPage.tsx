// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
