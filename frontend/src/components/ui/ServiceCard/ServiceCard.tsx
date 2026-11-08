// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import type {ServiceHealth} from '../../../types/systemHealth'
import StatusBadge from '../StatusBadge/StatusBadge'
import styles from './ServiceCard.module.css'

interface ServiceCardProps {
    service: ServiceHealth
    formatName?: (value: string) => string
}

export default function ServiceCard({
                                        service,
                                        formatName = (value) => value,
                                    }: ServiceCardProps) {
    const isAvailable =
        service.reachable && service.status === 'UP'

    const serviceName = formatName(service.service)

    const responseTime =
        service.responseTimeMs != null
            ? `${service.responseTimeMs} ms`
            : 'Nicht verfügbar'

    return (
        <article
            className={[
                styles.card,
                isAvailable
                    ? styles.available
                    : styles.unavailable,
            ].join(' ')}
            aria-label={`${serviceName}: ${
                isAvailable
                    ? 'betriebsbereit'
                    : 'gestört'
            }`}
        >
            <div className={styles.header}>
                <span
                    className={[
                        styles.statusIndicator,
                        isAvailable
                            ? styles.statusIndicatorAvailable
                            : styles.statusIndicatorUnavailable,
                    ].join(' ')}
                    aria-hidden="true"
                />

                <StatusBadge
                    available={isAvailable}
                    label={
                        isAvailable
                            ? 'Betriebsbereit'
                            : service.status
                    }
                />
            </div>

            <h3 className={styles.title}>
                {serviceName}
            </h3>

            <dl className={styles.details}>
                <div className={styles.detail}>
                    <dt className={styles.detailName}>
                        Erreichbarkeit
                    </dt>

                    <dd className={styles.detailValue}>
                        {service.reachable
                            ? 'Erreichbar'
                            : 'Nicht erreichbar'}
                    </dd>
                </div>

                <div className={styles.detail}>
                    <dt className={styles.detailName}>
                        Antwortzeit
                    </dt>

                    <dd className={styles.detailValue}>
                        {responseTime}
                    </dd>
                </div>
            </dl>
        </article>
    )
}