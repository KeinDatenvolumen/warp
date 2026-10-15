import styles from './StatusBadge.module.css'

interface StatusBadgeProps {
    available: boolean
    label: string
}

export default function StatusBadge({
                                        available,
                                        label
                                    }: StatusBadgeProps) {
    return (
        <>
      <span
          className={`${styles.dot} ${
              available ? styles.dotAvailable : styles.dotUnavailable
          }`}
          aria-hidden="true"
      />

            <span
                className={`${styles.badge} ${
                    available
                        ? styles.badgeAvailable
                        : styles.badgeUnavailable
                }`}
            >
        {label}
      </span>
        </>
    )
}