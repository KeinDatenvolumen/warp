import type { DepartureSlotResult, OptimalWindowSummary } from '../../types/weatherOptimization'

/**
 * Berechnet in O(N * M) alle Abfahrtszeitpunkte aus der vorberechneten
 * (N Segmente x T Zeitslots) Wetter-Score-Matrix ohne erneute API-Calls.
 * (Gemäß WARP Single-Fetch Matrix-Batching & Sliding-Window-Faltung)
 */
export function findOptimalDepartureTimes(
    segmentScoreMatrix: number[][], // [segmentIdx][timeSlotIdx] -> Score 0..100
    segmentReasonMatrix: string[][], // [segmentIdx][timeSlotIdx] -> Reason Label
    segmentThunderMatrix: boolean[][], // [segmentIdx][timeSlotIdx] -> FR-WETT-03 Flag
    segmentDurationsMin: number[], // Dauer jedes Segments in Minuten
    timeSlotIsos: string[], // 15-min Zeitstempel des Open-Meteo Arrays
    isDayArray: boolean[], // Open-Meteo is_day für Ziel-Koordinate
    windowStartSlotIdx: number, // Index des frühesten Starts (z.B. 07:00)
    windowEndSlotIdx: number, // Index des spätesten Starts (z.B. 19:00)
    profile: 'BICYCLE' | 'MOTORCYCLE' | 'HIKING' = 'BICYCLE',
): { slots: DepartureSlotResult[]; bestSlotIndex: number; optimalWindowSummary: OptimalWindowSummary } {
    const numSegments = segmentDurationsMin.length
    const totalDurationMin = segmentDurationsMin.reduce((a, b) => a + b, 0)
    const tau = profile === 'MOTORCYCLE' ? 12.0 : profile === 'BICYCLE' ? 14.0 : 15.0
    const nightPenalty = profile === 'HIKING' ? 0.25 : profile === 'BICYCLE' ? 0.55 : 0.65

    // 1. Vorberechnung der zeitlichen Slot-Offsets delta_i (in 15-min Schritten) je Segment
    const segmentSlotOffsets: number[] = []
    const segmentWeights: number[] = []
    let accMin = 0
    for (let i = 0; i < numSegments; i++) {
        const segDur = segmentDurationsMin[i] ?? 0
        const midSegmentMin = accMin + segDur / 2
        segmentSlotOffsets.push(Math.round(midSegmentMin / 15))
        segmentWeights.push(totalDurationMin > 0 ? segDur / totalDurationMin : 1 / numSegments)
        accMin += segDur
    }
    const totalSlotsOffset = Math.ceil(totalDurationMin / 15)

    // 2. Sliding-Window über alle M möglichen Abfahrtszeitpunkte t_0
    const rawSlots: DepartureSlotResult[] = []

    for (let t0 = windowStartSlotIdx; t0 <= windowEndSlotIdx; t0++) {
        const segScores: number[] = []
        const segReasons: string[] = []
        let hasThunderCap = false
        let minSegScore = 100
        let linearMean = 0

        for (let i = 0; i < numSegments; i++) {
            const slotOffset = segmentSlotOffsets[i] ?? 0
            const slotIdx = Math.min(t0 + slotOffset, timeSlotIsos.length - 1)
            const s = segmentScoreMatrix[i]?.[slotIdx] ?? 50
            const reason = segmentReasonMatrix[i]?.[slotIdx] ?? 'Normal'
            segScores.push(s)
            segReasons.push(reason)

            if (segmentThunderMatrix[i]?.[slotIdx]) {
                hasThunderCap = true
            }
            if (s < minSegScore) {
                minSegScore = s
            }

            linearMean += (segmentWeights[i] ?? 0) * s
        }

        // Dreistufige WARP Routen-Aggregation (Log-Sum-Exp shifted by minSegScore for numerical stability)
        let expSum = 0
        for (let i = 0; i < numSegments; i++) {
            const s = segScores[i] ?? minSegScore
            const w = segmentWeights[i] ?? 0
            expSum += w * Math.exp(-(s - minSegScore) / tau)
        }
        const softMin = minSegScore - tau * Math.log(Math.max(1e-15, expSum))
        const bottleneckCap = minSegScore + 0.45 * Math.sqrt(minSegScore * (100 - minSegScore))
        const thunderCap = hasThunderCap ? 50 : 100

        let routeScore = Math.min(bottleneckCap, thunderCap, 0.35 * linearMean + 0.65 * softMin)

        // Tageslicht-Prüfung am Zielpunkt
        const arrivalSlotIdx = Math.min(t0 + totalSlotsOffset, timeSlotIsos.length - 1)
        const reachesNightfall = !isDayArray[arrivalSlotIdx] || !isDayArray[t0]
        if (reachesNightfall) {
            routeScore *= nightPenalty
        }

        const depIso = timeSlotIsos[t0] ?? ''
        const arrIso = timeSlotIsos[arrivalSlotIdx] ?? ''

        rawSlots.push({
            departureIso: depIso,
            departureTime: formatSlotTime(depIso),
            arrivalIso: arrIso,
            arrivalTime: formatSlotTime(arrIso),
            routeScore: Math.round(Math.max(0, Math.min(100, routeScore))),
            robustScore: 0,
            hasThunderCap,
            reachesNightfall,
            segmentScores: segScores,
            segmentReasons: segReasons,
            summaryDeltaText: '',
        })
    }

    if (rawSlots.length === 0) {
        return {
            slots: [],
            bestSlotIndex: 0,
            optimalWindowSummary: {
                bestDeparture: '',
                bestDepartureIso: '',
                bestScore: 0,
                stableWindow: '',
                advantageVsWorst: '',
                worstDeparture: '',
                worstScore: 0,
            },
        }
    }

    // 3. Plateau-Stabilitäts-Filter (Gauß-Glättung [0.2, 0.6, 0.2]) & Best-Slot-Ermittlung
    let bestIdx = 0
    let maxRobust = -1
    let worstIdx = 0
    let minScore = 101

    for (let m = 0; m < rawSlots.length; m++) {
        const prev = rawSlots[Math.max(0, m - 1)]?.routeScore ?? 0
        const curr = rawSlots[m]?.routeScore ?? 0
        const next = rawSlots[Math.min(rawSlots.length - 1, m + 1)]?.routeScore ?? 0

        const robust = Math.round((0.2 * prev + 0.6 * curr + 0.2 * next) * 10) / 10
        const slot = rawSlots[m]
        if (slot) {
            slot.robustScore = robust
        }

        if (robust > maxRobust) {
            maxRobust = robust
            bestIdx = m
        }

        if (curr < minScore) {
            minScore = curr
            worstIdx = m
        }
    }

    const firstSlot = rawSlots[0]!
    const bestSlot = rawSlots[bestIdx] ?? firstSlot
    const worstSlot = rawSlots[worstIdx] ?? firstSlot

    // Stabiles Fenster (z.B. >= bestScore - 6 und >= 50)
    let stableStart = bestIdx
    let stableEnd = bestIdx
    const threshold = Math.max(50, bestSlot.routeScore - 6)
    while (stableStart > 0 && (rawSlots[stableStart - 1]?.routeScore ?? 0) >= threshold) {
        stableStart--
    }
    while (stableEnd < rawSlots.length - 1 && (rawSlots[stableEnd + 1]?.routeScore ?? 0) >= threshold) {
        stableEnd++
    }

    const diff = bestSlot.routeScore - worstSlot.routeScore
    let advantage = `+${diff} Punkte ggü. ${worstSlot.departureTime} Uhr`
    if (worstSlot.hasThunderCap && !bestSlot.hasThunderCap) {
        advantage += ' (umgeht Gewitterfront)'
    } else if (worstSlot.reachesNightfall && !bestSlot.reachesNightfall) {
        advantage += ' (nutzt volles Tageslicht)'
    } else {
        advantage += ' (optimale Wetterkonditionen)'
    }

    const startSlot = rawSlots[stableStart] ?? firstSlot
    const endSlot = rawSlots[stableEnd] ?? firstSlot

    const optimalWindowSummary: OptimalWindowSummary = {
        bestDeparture: bestSlot.departureTime,
        bestDepartureIso: bestSlot.departureIso,
        bestScore: bestSlot.routeScore,
        stableWindow: `${startSlot.departureTime} – ${endSlot.departureTime} Uhr`,
        advantageVsWorst: advantage,
        worstDeparture: worstSlot.departureTime,
        worstScore: worstSlot.routeScore,
    }

    for (let m = 0; m < rawSlots.length; m++) {
        const slot = rawSlots[m]
        if (slot) {
            const diffToWorst = slot.routeScore - worstSlot.routeScore
            slot.summaryDeltaText =
                diffToWorst > 0 ? `+${diffToWorst} Pkt. ggü. ${worstSlot.departureTime} Uhr` : 'Tiefstwert im Zeitfenster'
        }
    }

    return { slots: rawSlots, bestSlotIndex: bestIdx, optimalWindowSummary }
}

export function formatSlotTime(isoString: string): string {
    if (!isoString) return ''
    try {
        const date = new Date(isoString)
        if (Number.isNaN(date.getTime())) return ''
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    } catch {
        return ''
    }
}
