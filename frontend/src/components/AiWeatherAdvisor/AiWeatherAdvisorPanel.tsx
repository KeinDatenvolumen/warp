import { Link } from 'react-router'
import type {
    AiRiskLevel,
    RouteWeatherAiAnalysisResponse,
} from '../../types/weatherAiAnalysis'
import styles from './AiWeatherAdvisorPanel.module.css'

export interface AiWeatherAdvisorPanelProps {
    analysis: RouteWeatherAiAnalysisResponse | null
    loading: boolean
    error: string | null
    errorCode?: string | null
    isAuthenticated: boolean
    onRefresh: () => void
    onHoverSegment?: (segmentIndex: number | null) => void
}

function resolveRiskBadge(riskLevel: AiRiskLevel): {
    label: string
    className: string
} {
    switch (riskLevel) {
        case 'LOW':
            return {
                label: 'Gute Bedingungen',
                className: styles.riskLow ?? '',
            }
        case 'MODERATE':
            return {
                label: 'Wechselhaft',
                className: styles.riskModerate ?? '',
            }
        case 'HIGH':
            return {
                label: 'Erschwerte Bedingungen',
                className: styles.riskHigh ?? '',
            }
        case 'CRITICAL':
            return {
                label: 'Wetterwarnung',
                className: styles.riskCritical ?? '',
            }
    }
}

export default function AiWeatherAdvisorPanel({
    analysis,
    loading,
    error,
    errorCode,
    isAuthenticated,
    onRefresh,
}: AiWeatherAdvisorPanelProps) {
    if (!isAuthenticated) {
        return (
            <section
                className={styles.panel}
                aria-label="Wetter-Zusammenfassung"
            >
                <header className={styles.header}>
                    <div className={styles.titleGroup}>
                        <p className={styles.eyebrow}>Wetteranalyse</p>
                        <h2 className={styles.title}>
                            Wetter-Zusammenfassung zur Route
                        </h2>
                    </div>
                </header>
                <p className={styles.noticeText}>
                    Die ausführliche Wetter-Zusammenfassung steht für
                    angemeldete und verifizierte Nutzer bereit.{' '}
                    <Link to="/login" className={styles.inlineLink}>
                        Zum Login
                    </Link>
                    .
                </p>
            </section>
        )
    }

    const riskBadge = analysis
        ? resolveRiskBadge(analysis.riskLevel)
        : null

    return (
        <section
            className={styles.panel}
            aria-label="Wetter-Zusammenfassung"
        >
            <header className={styles.header}>
                <div className={styles.titleGroup}>
                    <p className={styles.eyebrow}>Wetteranalyse</p>
                    <h2 className={styles.title}>
                        Wetter-Zusammenfassung zur Route
                    </h2>
                </div>

                <div className={styles.headerActions}>
                    {riskBadge && (
                        <span
                            className={`${styles.riskBadge} ${riskBadge.className}`}
                        >
                            {riskBadge.label}
                        </span>
                    )}
                    <button
                        type="button"
                        className={styles.refreshButton}
                        disabled={loading}
                        onClick={onRefresh}
                    >
                        {loading
                            ? 'Wird aktualisiert …'
                            : 'Aktualisieren'}
                    </button>
                </div>
            </header>

            {loading && !analysis && (
                <div
                    className={styles.stateBox}
                    role="status"
                    aria-live="polite"
                >
                    Die Wetterdaten entlang deiner Route werden
                    zusammengefasst …
                </div>
            )}

            {error && (
                <div
                    className={`${styles.stateBox} ${
                        errorCode === 'EMAIL_NOT_VERIFIED'
                            ? styles.stateBoxWarning
                            : styles.stateBoxError
                    }`}
                    role="alert"
                >
                    {error}
                </div>
            )}

            {analysis && (
                <div className={styles.contentBlock}>
                    {analysis.headline && (
                        <h3 className={styles.headline}>
                            {analysis.headline}
                        </h3>
                    )}

                    <p className={styles.summaryText}>
                        {analysis.overallSummary}
                    </p>

                    {analysis.recommendations.length > 0 && (
                        <div className={styles.recommendationsBlock}>
                            <h4 className={styles.recommendationsTitle}>
                                Hinweise für die Tour
                            </h4>
                            <ul className={styles.recommendationsList}>
                                {analysis.recommendations.map(
                                    (rec, idx) => (
                                        <li key={idx}>{rec}</li>
                                    ),
                                )}
                            </ul>
                        </div>
                    )}
                </div>
            )}
        </section>
    )
}
