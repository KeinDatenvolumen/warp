import styles from './MessageCard.module.css'

export default function MessageCard({children, error = false}) {
    return <section className={`${styles.card} ${error ? styles.error : ''}`}
                    role={error ? 'alert' : undefined}>{children}</section>
}
