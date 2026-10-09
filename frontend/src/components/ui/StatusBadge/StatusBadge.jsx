import styles from './StatusBadge.module.css'

export default function StatusBadge({available, label}) {
    return <><span className={`${styles.dot} ${available ? styles.dotAvailable : styles.dotUnavailable}`}
                   aria-hidden="true"/><span
        className={`${styles.badge} ${available ? styles.badgeAvailable : styles.badgeUnavailable}`}>{label}</span></>
}
