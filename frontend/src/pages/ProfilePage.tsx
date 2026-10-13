import styles from './PagePlaceholder.module.css'

export default function ProfilePage() {
    return <main className={styles.page}>
        <section className={styles.card}><p className={styles.eyebrow}>Benutzerkonto</p><h1>Profil</h1><p
            className={styles.description}>Hier werden später die Profildaten des Benutzers angezeigt.</p></section>
    </main>
}
