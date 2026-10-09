import styles from './PagePlaceholder.module.css'

export default function SavedRoutesPage() {
    return <main className={styles.page}>
        <section className={styles.card}><p className={styles.eyebrow}>Routenarchiv</p><h1>Gespeicherte Routen</h1><p
            className={styles.description}>Hier werden später die gespeicherten Routen des Benutzers angezeigt.</p>
        </section>
    </main>
}
