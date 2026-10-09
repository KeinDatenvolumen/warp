import StatusBadge from '../StatusBadge/StatusBadge.jsx'
import styles from './ServiceCard.module.css'

export default function ServiceCard({service, formatName = value => value}) {
    const isAvailable = service.reachable && service.status === 'UP'
    return (
        <article className={`${styles.card} ${isAvailable ? styles.available : styles.unavailable}`}>
            <div className={styles.header}>
                <StatusBadge available={isAvailable} label={isAvailable ? 'Healthy' : service.status}/>
            </div>
            <h2 className={styles.title}>{formatName(service.service)}</h2>
            <dl className={styles.details}>
                <div className={styles.detail}>
                    <dt className={styles.detailName}>Erreichbarkeit</dt>
                    <dd className={styles.detailValue}>{service.reachable ? 'Erreichbar' : 'Nicht erreichbar'}</dd>
                </div>
                <div className={styles.detail}>
                    <dt className={styles.detailName}>Antwortzeit</dt>
                    <dd className={styles.detailValue}>{service.responseTimeMs ?? 'Nicht verfügbar'}{service.responseTimeMs != null ? ' ms' : ''}</dd>
                </div>
            </dl>
        </article>
    )
}
