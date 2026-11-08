// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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