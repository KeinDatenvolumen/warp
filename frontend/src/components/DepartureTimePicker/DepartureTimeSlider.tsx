// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-242] (a913a41) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: optimiere Routenplaner, Abfahrtszeit-Slider und gespeicherte Routen (WARP-014)
import {
    useState,
    type ChangeEvent,
} from 'react'
import type {
    DepartureSlotResult,
    OptimalWindowSummary,
} from '../../types/weatherOptimization'
import { getScoreColor } from '../../utils/weatherColorScale'
import styles from './DepartureTimeSlider.module.css'

export interface DepartureTimeSliderProps {
    slots: DepartureSlotResult[]
    recommendedSlotIndex: number
    selectedSlotIndex: number
    optimalWindowSummary?: OptimalWindowSummary | null
    initialDepartureIndex?: number
    onSelectSlot: (slotIndex: number) => void
}

export default function DepartureTimeSlider({
    slots,
    recommendedSlotIndex,
    selectedSlotIndex,
    optimalWindowSummary,
    initialDepartureIndex = 0,
    onSelectSlot,
}: DepartureTimeSliderProps) {
    const [showCarpetPlot, setShowCarpetPlot] = useState(false)

    if (!slots || slots.length === 0) {
        return null
    }

    const firstSlot = slots[0]!
    const activeSlot = slots[selectedSlotIndex] ?? firstSlot
    const bestSlot = slots[recommendedSlotIndex] ?? firstSlot
    const initialSlot = slots[initialDepartureIndex] ?? firstSlot

    // Delta calculation: Selected vs Initial planned departure
    const deltaToInitial = activeSlot.routeScore - initialSlot.routeScore

    function handleSliderChange(e: ChangeEvent<HTMLInputElement>) {
        const val = Number.parseInt(e.target.value, 10)
        if (!Number.isNaN(val) && val >= 0 && val < slots.length) {
            onSelectSlot(val)
        }
    }

    return (
        <section
            className={styles.container}
            aria-label="WARP Abfahrtszeit-Optimierer"
        >
            <header className={styles.header}>
                <div className={styles.titleGroup}>
                    <span className={styles.eyebrow}>
                        Startzeit-Empfehlung
                    </span>
                    <h2 className={styles.title}>Abfahrtszeit-Optimierung</h2>
                </div>

                <div className={styles.badgeOptimum}>
                    <span className={styles.pulseDot} />
                    <span>Optimum: {bestSlot.departureTime} Uhr (Score {bestSlot.routeScore})</span>
                </div>
            </header>

            {optimalWindowSummary && (
                <div className={styles.summaryCard}>
                    <div className={styles.summaryCol}>
                        <span className={styles.summaryLabel}>Ideales Startfenster</span>
                        <span className={styles.summaryValue}>
                            {optimalWindowSummary.stableWindow}
                        </span>
                    </div>

                    <div className={styles.summaryCol}>
                        <span className={styles.summaryLabel}>Bester Score</span>
                        <span className={styles.summaryValue} style={{ color: getScoreColor(optimalWindowSummary.bestScore) }}>
                            {optimalWindowSummary.bestScore} / 100
                        </span>
                    </div>

                    <div className={styles.summaryCol}>
                        <span className={styles.summaryLabel}>Vorteil</span>
                        <span className={styles.advantageText}>
                            {optimalWindowSummary.advantageVsWorst}
                        </span>
                    </div>

                    <div className={styles.quickActions}>
                        <button
                            type="button"
                            className={`${styles.actionButton} ${styles.actionButtonPrimary}`}
                            onClick={() => onSelectSlot(recommendedSlotIndex)}
                        >
                            Besten Startzeitpunkt wählen ({bestSlot.departureTime})
                        </button>

                        {initialDepartureIndex !== selectedSlotIndex && (
                            <button
                                type="button"
                                className={styles.actionButton}
                                onClick={() => onSelectSlot(initialDepartureIndex)}
                            >
                                Zurück zur geplanten Zeit ({initialSlot.departureTime})
                            </button>
                        )}

                        <button
                            type="button"
                            className={styles.actionButton}
                            onClick={() => setShowCarpetPlot((prev) => !prev)}
                            aria-expanded={showCarpetPlot}
                        >
                            {showCarpetPlot ? 'Carpet Plot ausblenden' : '2D Raum-Zeit-Matrix'}
                        </button>
                    </div>
                </div>
            )}

            {/* Bargraph / Departure Slot Picker */}
            <div className={styles.bargraphContainer}>
                <div className={styles.bargraphScrollWrapper}>
                    <div className={styles.bargraphInner}>
                        <div
                            className={styles.bargraphTrack}
                            role="listbox"
                            aria-label="Abfahrtszeitpunkte"
                        >
                            {slots.map((slot, idx) => {
                                const isSelected = idx === selectedSlotIndex
                                const isBest = idx === recommendedSlotIndex
                                const score = Math.max(0, Math.min(100, slot.routeScore))
                                const barHeightPercent = Math.max(12, score)
                                const color = getScoreColor(score)

                                return (
                                    <div
                                        key={slot.departureIso || idx}
                                        className={`${styles.barSlot} ${isSelected ? styles.barSlotActive : ''}`}
                                        role="option"
                                        aria-selected={isSelected}
                                        title={`Start ${slot.departureTime} Uhr (ETA ${slot.arrivalTime}) · Score ${score}${slot.hasThunderCap ? ' · Gewitterrisiko' : ''}${slot.reachesNightfall ? ' · Dämmerung' : ''}`}
                                        onClick={() => onSelectSlot(idx)}
                                    >
                                        {isBest && (
                                            <div className={styles.barMarkerPin}>
                                                BEST
                                            </div>
                                        )}

                                        <div
                                            className={`${styles.barFill} ${slot.reachesNightfall ? styles.barNightfall : ''}`}
                                            style={{
                                                height: `${barHeightPercent}%`,
                                                backgroundColor: color,
                                            }}
                                        />
                                    </div>
                                )
                            })}
                        </div>

                        {/* Separate timeline axis track directly beneath baseline */}
                        <div className={styles.timelineAxis} aria-hidden="true">
                            {slots.map((slot, idx) => {
                                const isSelected = idx === selectedSlotIndex
                                const isBest = idx === recommendedSlotIndex
                                const showLabel = idx % 4 === 0 || isSelected || isBest

                                return (
                                    <div
                                        key={slot.departureIso || idx}
                                        className={styles.timelineSlot}
                                        onClick={() => onSelectSlot(idx)}
                                    >
                                        <div
                                            className={`${styles.tick} ${idx % 4 === 0 ? styles.majorTick : ''} ${isSelected ? styles.activeTick : ''}`}
                                        />
                                        {showLabel && (
                                            <span
                                                className={`${styles.slotTimeLabel} ${isSelected ? styles.slotTimeLabelActive : ''}`}
                                            >
                                                {slot.departureTime}
                                            </span>
                                        )}
                                    </div>
                                )
                            })}
                        </div>
                    </div>
                </div>
            </div>

            {/* Range Slider for scrubbing */}
            <div className={styles.sliderControlWrapper}>
                <input
                    type="range"
                    className={styles.scrubSlider}
                    min={0}
                    max={slots.length - 1}
                    value={selectedSlotIndex}
                    onChange={handleSliderChange}
                    aria-label="Abfahrtszeit-Schieberegler"
                />
                <div className={styles.sliderLabels}>
                    <span>{slots[0]?.departureTime} Uhr</span>
                    <span>
                        Ausgewählt: <strong>{activeSlot?.departureTime} Uhr</strong> (Ankunft: {activeSlot?.arrivalTime} Uhr)
                    </span>
                    <span>{slots[slots.length - 1]?.departureTime} Uhr</span>
                </div>
            </div>

            {/* Live Delta Details for currently selected slot */}
            {activeSlot && (
                <div className={styles.selectionDetails}>
                    <div className={styles.selectionScoreBadge}>
                        <span>Abfahrt {activeSlot.departureTime} Uhr</span>
                        <span>·</span>
                        <span
                            className={styles.scorePill}
                            style={{ backgroundColor: getScoreColor(activeSlot.routeScore) }}
                        >
                            Score {activeSlot.routeScore} / 100
                        </span>
                        {activeSlot.hasThunderCap && (
                            <span style={{ color: '#ef4444', fontSize: '0.85rem' }}>
                                Gewitterrisiko (Score max. 50)
                            </span>
                        )}
                        {activeSlot.reachesNightfall && (
                            <span style={{ color: '#64748b', fontSize: '0.85rem' }}>
                                Ankunft in der Dämmerung
                            </span>
                        )}
                    </div>

                    <div className={styles.deltaText}>
                        {selectedSlotIndex === initialDepartureIndex ? (
                            <span className={styles.deltaNeutral}>Geplante Abfahrtszeit</span>
                        ) : deltaToInitial > 0 ? (
                            <span className={styles.deltaPositive}>
                                Start um {activeSlot.departureTime} statt {initialSlot.departureTime}: <strong>+{deltaToInitial} Punkte</strong>
                            </span>
                        ) : (
                            <span className={styles.deltaNegative}>
                                Start um {activeSlot.departureTime} statt {initialSlot.departureTime}: <strong>{deltaToInitial} Punkte</strong>
                            </span>
                        )}
                    </div>
                </div>
            )}

            {/* Optional 2D Carpet Plot (Raum-Zeit Mini-Heatmap) */}
            {showCarpetPlot && activeSlot?.segmentScores && activeSlot.segmentScores.length > 0 && (
                <div className={styles.carpetPlotWrapper}>
                    <div className={styles.carpetHeader}>
                        <span>2D Raum-Zeit-Matrix (Carpet Plot)</span>
                        <span>X: Abfahrtszeit · Y: Streckensegmente (Start → Ziel)</span>
                    </div>

                    <div className={styles.carpetScrollArea}>
                        <div
                            className={styles.carpetGrid}
                            style={{
                                gridTemplateColumns: `repeat(${slots.length}, 14px)`,
                                gridTemplateRows: `repeat(${activeSlot.segmentScores.length}, 12px)`,
                            }}
                        >
                            {activeSlot.segmentScores.map((_, segIdx) =>
                                slots.map((slot, sIdx) => {
                                    const segScore = slot.segmentScores?.[segIdx] ?? 50
                                    const cellColor = getScoreColor(segScore)
                                    const isCurrentSlot = sIdx === selectedSlotIndex

                                    return (
                                        <div
                                            key={`${segIdx}-${sIdx}`}
                                            className={styles.carpetCell}
                                            style={{
                                                backgroundColor: cellColor,
                                                outline: isCurrentSlot ? '1px solid #000' : 'none',
                                            }}
                                            title={`Segment ${segIdx + 1} bei Start ${slot.departureTime}: Score ${segScore} (${slot.segmentReasons?.[segIdx] ?? ''})`}
                                            onClick={() => onSelectSlot(sIdx)}
                                        />
                                    )
                                }),
                            )}
                        </div>
                    </div>
                </div>
            )}
        </section>
    )
}
