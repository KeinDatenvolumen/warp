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

function resolveWindOrientation(
    reasonLabel?: string | null,
    limitingFactor?: string | null,
): { arrow: string; label: string; color: string } {
    const text = (reasonLabel ?? '').toLowerCase()

    if (text.includes('rückenwind') || limitingFactor === 'TAILWIND_BOOST') {
        return { arrow: '↑', label: 'Rückenwind', color: '#0284c7' }
    }
    if (text.includes('gegenwind')) {
        return { arrow: '↓', label: 'Gegenwind', color: '#dc2626' }
    }
    if (text.includes('seitenwind')) {
        return { arrow: '↔', label: 'Seitenwind', color: '#d97706' }
    }
    return { arrow: '↗', label: 'Wind', color: 'var(--color-text-muted)' }
}

export default function MeteogramDock({
    segments,
    totalDistanceMeters,
    hoveredSegmentIndex,
    onHoverSegment,
    onSelectSegment,
}: MeteogramDockProps) {
    const [isExpanded, setIsExpanded] = useState(true)
    const [pinnedSegmentIndex, setPinnedSegmentIndex] = useState<number | null>(null)

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

    const effectiveSegmentIndex = hoveredSegmentIndex ?? pinnedSegmentIndex

    const activeSegment = useMemo(() => {
        if (effectiveSegmentIndex === null) {
            return null
        }
        return sortedSegments.find((s) => s.segmentIndex === effectiveSegmentIndex) ?? null
    }, [effectiveSegmentIndex, sortedSegments])

    // Gleichmäßig auf die N Segment-Spalten zentrierte Punkte, damit Kurve, Klickspots und Spuren exakt fluchten
    const chartPoints = useMemo(() => {
        const count = sortedSegments.length
        if (count === 0) return []

        const width = 1000
        const height = 80
        const paddingBottom = 8
        const paddingTop = 8
        const usableHeight = height - paddingBottom - paddingTop

        return sortedSegments.map((seg, idx) => {
            const xRatio = (idx + 0.5) / count
            const x = xRatio * width
            const rawScore = Number.isFinite(seg.score)
                ? seg.score
                : Number.isFinite((seg as unknown as { segmentScore?: number }).segmentScore)
                  ? (seg as unknown as { segmentScore?: number }).segmentScore!
                  : 50
            const clampedScore = Math.max(0, Math.min(100, rawScore))
            const y = height - paddingBottom - (clampedScore / 100) * usableHeight
            const yPercent = (y / height) * 100
            return {
                x,
                y,
                yPercent,
                score: clampedScore,
                color: getScoreColor(clampedScore),
                segmentIndex: seg.segmentIndex,
                segment: seg,
            }
        })
    }, [sortedSegments])

    const svgPaths = useMemo(() => {
        if (chartPoints.length === 0) return { linePath: '', areaPath: '', gradientStops: [] }

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

        const gradientStops = chartPoints.map((pt) => ({
            offset: `${(pt.x / 10).toFixed(1)}%`,
            color: pt.color,
        }))

        return { linePath: lineD, areaPath: areaD, gradientStops }
    }, [chartPoints])

    if (sortedSegments.length === 0) {
        return null
    }

    function handleSegmentHover(segmentIndex: number | null) {
        if (segmentIndex !== null) {
            onHoverSegment(segmentIndex)
        } else {
            onHoverSegment(pinnedSegmentIndex)
        }
    }

    function handleSegmentClick(segmentIndex: number) {
        onSelectSegment?.(segmentIndex)
        if (pinnedSegmentIndex === segmentIndex) {
            setPinnedSegmentIndex(null)
            onHoverSegment(null)
        } else {
            setPinnedSegmentIndex(segmentIndex)
            onHoverSegment(segmentIndex)
        }
    }

    return (
        <section
            className={styles.dockContainer}
            aria-label="Wetterverlauf entlang der Route"
        >
            <header className={styles.dockHeader}>
                <div className={styles.dockTitleGroup}>
                    <span className={styles.dockTitle}>Wetterverlauf entlang der Route</span>
                </div>

                {activeSegment ? (
                    <div className={styles.hoverInfoBadge} role="status">
                        <strong>Abschnitt {activeSegment.segmentIndex + 1}</strong>
                        <span>·</span>
                        <span>ab {formatKm(activeSegment.distanceFromStartMeters ?? 0)}</span>
                        {activeSegment.expectedArrivalTime && (
                            <span>({formatTime(activeSegment.expectedArrivalTime)} Uhr)</span>
                        )}
                        <span
                            className={styles.scorePill}
                            style={{
                                backgroundColor: getScoreColor(activeSegment.score),
                            }}
                        >
                            Score {activeSegment.score}
                        </span>
                        {activeSegment.weather?.temperatureCelsius != null && (
                            <span>{Math.round(activeSegment.weather.temperatureCelsius)} °C</span>
                        )}
                        {activeSegment.reasonLabel && (
                            <span className={styles.reasonHighlight}>
                                {activeSegment.reasonLabel}
                            </span>
                        )}
                        {pinnedSegmentIndex !== null && (
                            <button
                                type="button"
                                className={styles.unpinButton}
                                onClick={() => {
                                    setPinnedSegmentIndex(null)
                                    onHoverSegment(null)
                                }}
                            >
                                Auswahl aufheben
                            </button>
                        )}
                    </div>
                ) : (
                    <div className={styles.hoverInfoBadge}>
                        <span>Abschnitt berühren oder anklicken, um ihn auf der Karte hervorzuheben</span>
                    </div>
                )}

                <button
                    type="button"
                    className={styles.toggleButton}
                    onClick={() => setIsExpanded((prev) => !prev)}
                    aria-expanded={isExpanded}
                >
                    {isExpanded ? 'Einklappen' : 'Ausklappen'}
                </button>
            </header>

            {isExpanded && (
                <div
                    className={styles.dockBody}
                    onMouseLeave={() => handleSegmentHover(null)}
                >
                    {/* SPUR 1: Score-Kurve mit deckungsgleichen Klickspalten */}
                    <div className={styles.trackRow}>
                        <span className={styles.laneLabel}>Wetterscore</span>
                        <div className={styles.chartWrapper}>
                            <svg
                                className={styles.svgChart}
                                viewBox="0 0 1000 80"
                                preserveAspectRatio="none"
                                aria-hidden="true"
                            >
                                <defs>
                                    <linearGradient id="scoreAreaGradient" x1="0" y1="0" x2="1" y2="0">
                                        {svgPaths.gradientStops.map((stop, i) => (
                                            <stop
                                                key={i}
                                                offset={stop.offset}
                                                stopColor={stop.color}
                                                stopOpacity="0.3"
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

                                <line
                                    x1="0"
                                    y1="40"
                                    x2="1000"
                                    y2="40"
                                    className={styles.thresholdLine}
                                />

                                <path d={svgPaths.areaPath} fill="url(#scoreAreaGradient)" />
                                <path
                                    d={svgPaths.linePath}
                                    fill="none"
                                    stroke="url(#scoreStrokeGradient)"
                                    strokeWidth="3"
                                    strokeLinecap="round"
                                />
                            </svg>

                            <div className={styles.chartHitColumns}>
                                {chartPoints.map((pt) => {
                                    const isCurrent = effectiveSegmentIndex === pt.segmentIndex
                                    const isPinned = pinnedSegmentIndex === pt.segmentIndex
                                    return (
                                        <button
                                            key={pt.segmentIndex}
                                            type="button"
                                            className={`${styles.chartHitCol} ${isCurrent ? styles.chartHitColActive : ''}`}
                                            aria-label={`Abschnitt ${pt.segmentIndex + 1}, Score ${pt.score}`}
                                            aria-pressed={isPinned}
                                            onMouseEnter={() => handleSegmentHover(pt.segmentIndex)}
                                            onFocus={() => handleSegmentHover(pt.segmentIndex)}
                                            onClick={() => handleSegmentClick(pt.segmentIndex)}
                                        >
                                            <span
                                                className={`${styles.chartPointDot} ${isCurrent ? styles.chartPointDotActive : ''}`}
                                                style={{
                                                    top: `${pt.yPercent}%`,
                                                    backgroundColor: pt.color,
                                                }}
                                            />
                                        </button>
                                    )
                                })}
                            </div>
                        </div>
                    </div>

                    {/* SPUR 2: Niederschlag & Fahrbahnnässe */}
                    <div className={styles.trackRow}>
                        <span className={styles.laneLabel}>Niederschlag</span>
                        <div className={styles.laneSegmentsRow}>
                            {sortedSegments.map((seg) => {
                                const precipMm = seg.weather?.precipitationMm ?? 0
                                const rainPercent = Math.min(100, Math.round((precipMm / 5.0) * 100))
                                const wetRoad = seg.weather?.wetRoadIndex ?? 0
                                const isWet = wetRoad > 0.25
                                const isCurrent = effectiveSegmentIndex === seg.segmentIndex

                                return (
                                    <button
                                        key={seg.segmentIndex}
                                        type="button"
                                        className={`${styles.rainSegmentCol} ${isCurrent ? styles.segmentColActive : ''}`}
                                        title={`Niederschlag: ${precipMm.toFixed(1)} mm/h · Oberflächennässe: ${(wetRoad * 100).toFixed(0)} %`}
                                        onMouseEnter={() => handleSegmentHover(seg.segmentIndex)}
                                        onClick={() => handleSegmentClick(seg.segmentIndex)}
                                    >
                                        {precipMm > 0 && (
                                            <span
                                                className={styles.rainBarFill}
                                                style={{ height: `${Math.max(18, rainPercent * 0.75)}%` }}
                                            />
                                        )}
                                        <span
                                            className={`${styles.roadWetnessStrip} ${isWet ? styles.roadWetnessActive : ''}`}
                                        />
                                    </button>
                                )
                            })}
                        </div>
                    </div>

                    {/* SPUR 3: Wind & Böen */}
                    <div className={styles.trackRow}>
                        <span className={styles.laneLabel}>Wind (km/h)</span>
                        <div className={styles.laneSegmentsRow}>
                            {sortedSegments.map((seg) => {
                                const windSpeed = Math.round(seg.weather?.windSpeedKmh ?? 0)
                                const windGust = Math.round(seg.weather?.windGustsKmh ?? windSpeed)
                                const orientation = resolveWindOrientation(
                                    seg.reasonLabel,
                                    seg.limitingFactor,
                                )
                                const isCurrent = effectiveSegmentIndex === seg.segmentIndex

                                return (
                                    <button
                                        key={seg.segmentIndex}
                                        type="button"
                                        className={`${styles.windCol} ${isCurrent ? styles.segmentColActive : ''}`}
                                        title={`${orientation.label} · Mittelwind: ${windSpeed} km/h, Böen: ${windGust} km/h`}
                                        onMouseEnter={() => handleSegmentHover(seg.segmentIndex)}
                                        onClick={() => handleSegmentClick(seg.segmentIndex)}
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
                                    </button>
                                )
                            })}
                        </div>
                    </div>

                    {/* FOOTER / DISTANZACHSE & LEGENDE */}
                    <footer className={styles.dockFooter}>
                        <span className={styles.footerSpacer} />
                        <div className={styles.footerAxisRow}>
                            <span>0,0 km (Start)</span>
                            <div className={styles.legendList}>
                                <div className={styles.legendItem}>
                                    <span className={styles.legendDot} style={{ backgroundColor: '#52B788' }} />
                                    <span>Gut (80+)</span>
                                </div>
                                <div className={styles.legendItem}>
                                    <span className={styles.legendDot} style={{ backgroundColor: '#F6AE2D' }} />
                                    <span>Mäßig (50–79)</span>
                                </div>
                                <div className={styles.legendItem}>
                                    <span className={styles.legendDot} style={{ backgroundColor: '#C81D25' }} />
                                    <span>Ungünstig (&lt;50)</span>
                                </div>
                                <div className={styles.legendItem}>
                                    <span className={styles.legendWetBar} />
                                    <span>Nasse Fahrbahn</span>
                                </div>
                            </div>
                            <span>{formatKm(totalDistance)} (Ziel)</span>
                        </div>
                    </footer>
                </div>
            )}
        </section>
    )
}
