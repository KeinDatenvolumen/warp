// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
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