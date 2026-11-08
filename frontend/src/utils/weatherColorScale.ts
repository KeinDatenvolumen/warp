// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * Wahrnehmungslineare 101-Stufen-Farbskala für den WARP Wetter-Score (0-100).
 *
 * 7 Stützpunkte (Control Stops) im Oklch-Farbraum interpoliert:
 *  0:  #2A0845 (Tiefes Schwarz-Violett · Extremgefahr)
 * 15:  #7B0D4B (Dunkles Purpur-Magenta · Schweres Unwetter/Gewitter)
 * 30:  #C81D25 (Leuchtendes Karminrot · Sehr widrig)
 * 50:  #F26419 (Sattes Signal-Orange · [FR-WETT-03] Schwellenwert)
 * 68:  #F6AE2D (Warmes Bernstein-Gold · Mäßige Bedingungen)
 * 85:  #52B788 (Frisches Smaragdgrün · Sehr gute Bedingungen)
 * 100: #00B4D8 (Strahlendes Cyan-Türkis · Perfektes Traumwetter / Rückenwind)
 */
export const SCORE_COLORS: readonly string[] = Object.freeze([
    '#2A0845', '#2F0A46', '#340C46', '#390D47', '#3E0E47', '#430F48', '#491048', '#4E1049', '#531149', '#59114A',
    '#5E114A', '#64114A', '#6A104A', '#6F104B', '#750E4B', '#7B0D4B', '#800F4A', '#851148', '#8A1247', '#8F1445',
    '#941543', '#991642', '#9E1740', '#A3183D', '#A8193B', '#AD1A38', '#B31B35', '#B81B32', '#BD1C2E', '#C31D2A',
    '#C81D25', '#CA2225', '#CC2725', '#CE2B24', '#D12F24', '#D33324', '#D53723', '#D73B23', '#D93E23', '#DB4222',
    '#DD4522', '#DF4821', '#E14C20', '#E34F20', '#E6521F', '#E8551E', '#EA581D', '#EC5B1C', '#EE5E1B', '#F0611A',
    '#F26419', '#F3691A', '#F36D1B', '#F3721C', '#F4761E', '#F47A1F', '#F57E20', '#F58321', '#F58722', '#F58B23',
    '#F68F24', '#F69325', '#F69726', '#F69B28', '#F69F29', '#F6A22A', '#F6A62B', '#F6AA2C', '#F6AE2D', '#EEAF38',
    '#E7B040', '#DFB148', '#D7B24E', '#CFB355', '#C6B45A', '#BEB55F', '#B5B564', '#ACB669', '#A3B66D', '#9AB672',
    '#90B776', '#86B77A', '#7AB77D', '#6EB781', '#61B785', '#52B788', '#4FB78E', '#4CB794', '#48B799', '#45B79F',
    '#41B7A4', '#3DB7AA', '#39B6AF', '#35B6B4', '#31B6B9', '#2CB6BF', '#26B5C4', '#20B5C9', '#19B5CE', '#0FB4D3',
    '#00B4D8',
])

/**
 * Gibt den vorberechneten wahrnehmungslinearen Hex-Farbcode für einen ganzzahligen Score (0-100) zurück.
 */
export function getScoreColor(score: number): string {
    const clamped = Math.max(0, Math.min(100, Math.round(score)))
    return SCORE_COLORS[clamped] ?? '#F26419'
}

export interface ScoreCategory {
    label: string
    color: string
    textColor: string
    icon: string
}

export function getScoreCategory(score: number): ScoreCategory {
    const color = getScoreColor(score)
    if (score >= 85) {
        return {
            label: 'Traumhaft',
            color,
            textColor: '#0f172a',
            icon: '✨',
        }
    }
    if (score >= 68) {
        return {
            label: 'Gut',
            color,
            textColor: '#0f172a',
            icon: '☀️',
        }
    }
    if (score >= 50) {
        return {
            label: 'Mäßig',
            color,
            textColor: '#ffffff',
            icon: '⛅',
        }
    }
    if (score >= 30) {
        return {
            label: 'Widrig',
            color,
            textColor: '#ffffff',
            icon: '🌧️',
        }
    }
    if (score >= 15) {
        return {
            label: 'Sehr widrig',
            color,
            textColor: '#ffffff',
            icon: '⛈️',
        }
    }
    return {
        label: 'Gefährlich',
        color,
        textColor: '#ffffff',
        icon: '⚠️',
    }
}
