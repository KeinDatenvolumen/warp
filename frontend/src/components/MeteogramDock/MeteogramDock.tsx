import {
    useMemo,
    useState,
} from 'react'
import type { RouteWeatherSegment } from '../../types/routeCalculation'
import { getScoreColor } from '../../utils/weatherColorScale'
import styles from './MeteogramDock.module.css'

export interface MeteogramDockProps {
    segments: RouteWeatherSegment[]
    totalDistanceMeters?: number | null
    hoveredSegmentIndex: number | null
    onHoverSegment: (index: number | null) => void
    onSelectSegment?: (index: number) => void
    activityProfileCode?: string | null
}

function formatKm(meters: number): string {
    return `${(meters / 1000).toFixed(1)} km`
}

function formatTime(isoString: string): string {
    try {
        const date = new Date(isoString)
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    } catch {
        return ''
    }
}

/**
 * Ermittelt aus Windrichtung und relativer Fahrtrichtung das aerodynamische Symbol.
 * Pfeil nach unten = Gegenwind (rot/orange)
 * Pfeil quer       = Seitenwind (gelb/orange)
 * Pfeil nach oben  = Rückenwind (grün/cyan)
 */
function resolveWindOrientation(
    reasonLabel?: string | null,
    limitingFactor?: string | null,
): { arrow: string; label: string; color: string } {
    const text = (reasonLabel ?? '').toLowerCase()

    if (text.includes('rückenwind') || limitingFactor === 'TAILWIND_BOOST') {
        return { arrow: '↑', label: 'Rückenwind', color: '#00b4d8' }
    }
    if (text.includes('gegenwind')) {
        return { arrow: '↓', label: 'Gegenwind', color: '#ef4444' }
    }
    if (text.includes('seitenwind')) {
        return { arrow: '↔', label: 'Seitenwind', color: '#f59e0b' }
    }
    return { arrow: '↗', label: 'Wind', color: '#94a3b8' }
}

export default function MeteogramDock({
    segments,
    totalDistanceMeters,
    hoveredSegmentIndex,
    onHoverSegment,
    onSelectSegment,
}: MeteogramDockProps) {
    const [isExpanded, setIsExpanded] = useState(true)

    const sortedSegments = useMemo(() => {
        return [...segments].sort((a, b) => a.segmentIndex - b.segmentIndex)
    }, [segments])

    const totalDistance = useMemo(() => {
        if (totalDistanceMeters && totalDistanceMeters > 0) {
            return totalDistanceMeters
        }
        if (sortedSegments.length > 0) {
            const last = sortedSegments[sortedSegments.length - 1]
            return (last?.distanceFromStartMeters ?? 0) + 5000
        }
        return 10000
    }, [totalDistanceMeters, sortedSegments])

    const activeSegment = useMemo(() => {
        if (hoveredSegmentIndex === null) {
            return null
        }
        return sortedSegments.find((s) => s.segmentIndex === hoveredSegmentIndex) ?? null
    }, [hoveredSegmentIndex, sortedSegments])

    // Punkte für das SVG Chart (Spur 1: Score 0-100)
    const chartPoints = useMemo(() => {
        if (sortedSegments.length === 0) return []

        const width = 1000
        const height = 80
        const paddingBottom = 6
        const paddingTop = 6
        const usableHeight = height - paddingBottom - paddingTop

        return sortedSegments.map((seg, idx) => {
            const dist = seg.distanceFromStartMeters ?? idx * (totalDistance / sortedSegments.length)
            const x = Math.max(0, Math.min(width, (dist / totalDistance) * width))
            const clampedScore = Math.max(0, Math.min(100, seg.score))
            const y = height - paddingBottom - (clampedScore / 100) * usableHeight
            return {
                x,
                y,
                score: clampedScore,
                color: getScoreColor(clampedScore),
                segmentIndex: seg.segmentIndex,
            }
        })
    }, [sortedSegments, totalDistance])

    const svgPaths = useMemo(() => {
        if (chartPoints.length === 0) return { linePath: '', areaPath: '', gradientStops: [] }

        // Start und Endpunkte an Kanten angleichen
        const first = chartPoints[0]
        const last = chartPoints[chartPoints.length - 1]
        if (!first || !last) return { linePath: '', areaPath: '', gradientStops: [] }

        let lineD = `M 0,${first.y} L ${first.x},${first.y}`
        for (let i = 1; i < chartPoints.length; i++) {
            const prev = chartPoints[i - 1]
            const curr = chartPoints[i]
            if (!prev || !curr) continue
            const midX = (prev.x + curr.x) / 2
            lineD += ` C ${midX},${prev.y} ${midX},${curr.y} ${curr.x},${curr.y}`
        }
        lineD += ` L 1000,${last.y}`

        const areaD = `${lineD} L 1000,80 L 0,80 Z`

        // Gradient Stops entlang der X-Achse
        const gradientStops = chartPoints.map((pt) => ({
            offset: `${(pt.x / 10).toFixed(1)}%`,
            color: pt.color,
        }))

        return { linePath: lineD, areaPath: areaD, gradientStops }
    }, [chartPoints])

    if (sortedSegments.length === 0) {
        return null
    }

    return (
        <section
            className={`${styles.dockContainer} ${isExpanded ? styles.dockExpanded : styles.dockCollapsed}`}
            aria-label="Wetter Meteogramm Dock"
        >
            <header className={styles.dockHeader}>
                <div className={styles.dockTitleGroup}>
                    <span>🌤️ WARP Meteogramm</span>
                    <span className={styles.badgeLive}>
                        <span className={styles.pulseDot} />
                        101-Stufen Oklch
                    </span>
                </div>

                {activeSegment ? (
                    <div className={styles.hoverInfoBadge} role="status">
                        <span>Segment {activeSegment.segmentIndex + 1}</span>
                        <span>·</span>
                        <span>{formatKm(activeSegment.distanceFromStartMeters ?? 0)}</span>
                        <span>({formatTime(activeSegment.expectedArrivalTime)})</span>
                        <span
                            className={styles.scorePill}
                            style={{
                                backgroundColor: getScoreColor(activeSegment.score),
                            }}
                        >
                            Score {activeSegment.score}
                        </span>
                        {activeSegment.reasonLabel && (
                            <span className={styles.reasonHighlight}>
                                ⚠️ {activeSegment.reasonLabel}
                            </span>
                        )}
                    </div>
                ) : (
                    <div className={styles.hoverInfoBadge}>
                        <span>Bewege die Maus über die Segmente für Details</span>
                    </div>
                )}

                <button
                    type="button"
                    className={styles.toggleButton}
                    onClick={() => setIsExpanded((prev) => !prev)}
                    aria-expanded={isExpanded}
                    aria-label={isExpanded ? 'Meteogramm einklappen' : 'Meteogramm ausklappen'}
                >
                    {isExpanded ? '▼ Minimieren' : '▲ Meteogramm'}
                </button>
            </header>

            {isExpanded && (
                <div className={styles.dockBody}>
                    {/* SPUR 1: Score-Gradient Kurve */}
                    <div className={styles.chartWrapper}>
                        <svg
                            className={styles.svgChart}
                            viewBox="0 0 1000 80"
                            preserveAspectRatio="none"
                        >
                            <defs>
                                <linearGradient id="scoreAreaGradient" x1="0" y1="0" x2="1" y2="0">
                                    {svgPaths.gradientStops.map((stop, i) => (
                                        <stop
                                            key={i}
                                            offset={stop.offset}
                                            stopColor={stop.color}
                                            stopOpacity="0.4"
                                        />
                                    ))}
                                </linearGradient>
                                <linearGradient id="scoreStrokeGradient" x1="0" y1="0" x2="1" y2="0">
                                    {svgPaths.gradientStops.map((stop, i) => (
                                        <stop
                                            key={i}
                                            offset={stop.offset}
                                            stopColor={stop.color}
                                        />
                                    ))}
                                </linearGradient>
                            </defs>

                            {/* [FR-WETT-03] Horizontale 50-Punkte Schwelle */}
                            <line
                                x1="0"
                                y1="43"
                                x2="1000"
                                y2="43"
                                className={styles.thresholdLine}
                            />
                            <text x="996" y="40" className={styles.thresholdLabel}>
                                [FR-WETT-03] Max 50
                            </text>

                            {/* Flächenkurve und Kontur */}
                            <path d={svgPaths.areaPath} fill="url(#scoreAreaGradient)" />
                            <path
                                d={svgPaths.linePath}
                                fill="none"
                                stroke="url(#scoreStrokeGradient)"
                                strokeWidth="3"
                                strokeLinecap="round"
                            />

                            {/* Datenpunkte für Hover-Interaktion */}
                            {chartPoints.map((pt) => (
                                <circle
                                    key={pt.segmentIndex}
                                    cx={pt.x}
                                    cy={pt.y}
                                    r={hoveredSegmentIndex === pt.segmentIndex ? 6 : 3}
                                    fill={pt.color}
                                    stroke="#ffffff"
                                    strokeWidth={hoveredSegmentIndex === pt.segmentIndex ? 2 : 1}
                                    style={{ cursor: 'pointer', transition: 'r 0.15s' }}
                                    onMouseEnter={() => onHoverSegment(pt.segmentIndex)}
                                    onMouseLeave={() => onHoverSegment(null)}
                                    onClick={() => onSelectSegment?.(pt.segmentIndex)}
                                />
                            ))}
                        </svg>
                    </div>

                    {/* SPUR 2: Hybride Niederschlagsbalken & Nasse Fahrbahn W_road */}
                    <div className={styles.laneRainRoad}>
                        <span className={styles.laneLabel}>🌧️ Regen / Nässe</span>
                        <div className={styles.laneSegmentsRow}>
                            {sortedSegments.map((seg) => {
                                const precipMm = seg.weather?.precipitationMm ?? 0
                                const rainPercent = Math.min(100, Math.round((precipMm / 5.0) * 100))
                                const wetRoad = seg.weather?.wetRoadIndex ?? 0
                                const isWet = wetRoad > 0.25
                                const isCurrent = hoveredSegmentIndex === seg.segmentIndex

                                return (
                                    <div
                                        key={seg.segmentIndex}
                                        className={`${styles.rainSegmentCol} ${isCurrent ? styles.rainSegmentColActive : ''}`}
                                        title={`Regen: ${precipMm.toFixed(1)} mm/h | Fahrbahnnässe: ${(wetRoad * 100).toFixed(0)}%`}
                                        onMouseEnter={() => onHoverSegment(seg.segmentIndex)}
                                        onMouseLeave={() => onHoverSegment(null)}
                                        onClick={() => onSelectSegment?.(seg.segmentIndex)}
                                    >
                                        {precipMm > 0 && (
                                            <div
                                                className={styles.rainBarFill}
                                                style={{ height: `${Math.max(15, rainPercent * 0.7)}%` }}
                                            />
                                        )}
                                        <div
                                            className={`${styles.roadWetnessStrip} ${isWet ? styles.roadWetnessActive : ''}`}
                                        />
                                    </div>
                                )
                            })}
                        </div>
                    </div>

                    {/* SPUR 3: Aerodynamik & Relative Windpfeile */}
                    <div className={styles.laneWind}>
                        <span className={styles.laneLabel}>💨 Wind / Böen</span>
                        <div className={styles.laneSegmentsRow}>
                            {sortedSegments.map((seg) => {
                                const windSpeed = Math.round(seg.weather?.windSpeedKmh ?? 0)
                                const windGust = Math.round(seg.weather?.windGustsKmh ?? windSpeed)
                                const orientation = resolveWindOrientation(
                                    seg.reasonLabel,
                                    seg.limitingFactor,
                                )
                                const isCurrent = hoveredSegmentIndex === seg.segmentIndex

                                return (
                                    <div
                                        key={seg.segmentIndex}
                                        className={`${styles.windCol} ${isCurrent ? styles.windColActive : ''}`}
                                        title={`${orientation.label} · Mittel: ${windSpeed} km/h, Böen: ${windGust} km/h`}
                                        onMouseEnter={() => onHoverSegment(seg.segmentIndex)}
                                        onMouseLeave={() => onHoverSegment(null)}
                                        onClick={() => onSelectSegment?.(seg.segmentIndex)}
                                    >
                                        <span
                                            className={styles.windArrow}
                                            style={{ color: orientation.color }}
                                        >
                                            {orientation.arrow}
                                        </span>
                                        <span className={styles.windSpeedText}>
                                            {windSpeed}/{windGust}
                                        </span>
                                    </div>
                                )
                            })}
                        </div>
                    </div>

                    {/* FOOTER / LEGENDE */}
                    <footer className={styles.dockFooter}>
                        <span>Start: 0.0 km</span>
                        <div className={styles.legendList}>
                            <div className={styles.legendItem}>
                                <span className={styles.legendDot} style={{ backgroundColor: '#00B4D8' }} />
                                <span>100 Traum</span>
                            </div>
                            <div className={styles.legendItem}>
                                <span className={styles.legendDot} style={{ backgroundColor: '#52B788' }} />
                                <span>85 Gut</span>
                            </div>
                            <div className={styles.legendItem}>
                                <span className={styles.legendDot} style={{ backgroundColor: '#F6AE2D' }} />
                                <span>68 Mäßig</span>
                            </div>
                            <div className={styles.legendItem}>
                                <span className={styles.legendDot} style={{ backgroundColor: '#F26419' }} />
                                <span>50 Warnung</span>
                            </div>
                            <div className={styles.legendItem}>
                                <span className={styles.legendDot} style={{ backgroundColor: '#C81D25' }} />
                                <span>30 Widrig</span>
                            </div>
                            <div className={styles.legendItem}>
                                <span className={styles.legendDot} style={{ backgroundColor: '#2A0845' }} />
                                <span>0 Extrem</span>
                            </div>
                        </div>
                        <span>Ziel: {formatKm(totalDistance)}</span>
                    </footer>
                </div>
            )}
        </section>
    )
}
