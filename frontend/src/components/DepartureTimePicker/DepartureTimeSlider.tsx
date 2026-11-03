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
                    <div className={styles.chartWrapper}>
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
                                        {isSelected && !isBest && (
                                            <div className={styles.barSelectedPin}>
                                                AKTIV
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

                        {/* Baseline */}
                        <div className={styles.chartBaseline} />

                        {/* Dedicated Time Axis underneath the baseline */}
                        <div className={styles.timeAxis} aria-hidden="true">
                            {slots.map((slot, idx) => {
                                const showLabel =
                                    slots.length <= 8
                                        ? true
                                        : idx % 4 === 0

                                return (
                                    <div
                                        key={slot.departureIso || idx}
                                        className={styles.timeTickCol}
                                    >
                                        {showLabel && (
                                            <>
                                                <div className={styles.timeTickMark} />
                                                <span
                                                    className={`${styles.slotTimeLabel} ${idx === selectedSlotIndex ? styles.slotTimeLabelActive : ''}`}
                                                >
                                                    {slot.departureTime}
                                                </span>
                                            </>
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
                            <span className={styles.thunderNotice}>
                                Gewitterrisiko (Score max. 50)
                            </span>
                        )}
                        {activeSlot.reachesNightfall && (
                            <span className={styles.nightfallNotice}>
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
                                            className={`${styles.carpetCell} ${isCurrentSlot ? styles.carpetCellCurrent : ''}`}
                                            style={{
                                                backgroundColor: cellColor,
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
