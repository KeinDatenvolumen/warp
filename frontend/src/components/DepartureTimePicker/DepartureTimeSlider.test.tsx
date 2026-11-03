import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import DepartureTimeSlider from './DepartureTimeSlider'
import type { DepartureSlotResult, OptimalWindowSummary } from '../../types/weatherOptimization'

describe('DepartureTimeSlider', () => {
    const mockSlots: DepartureSlotResult[] = [
        {
            departureIso: '2026-05-16T08:00:00Z',
            departureTime: '08:00',
            arrivalIso: '2026-05-16T09:12:00Z',
            arrivalTime: '09:12',
            routeScore: 82,
            robustScore: 81.6,
            hasThunderCap: false,
            reachesNightfall: false,
            segmentScores: [85, 80, 81],
            segmentReasons: ['Sonne', 'Rückenwind', 'Trocken'],
            summaryDeltaText: '+30 Pkt.',
        },
        {
            departureIso: '2026-05-16T08:15:00Z',
            departureTime: '08:15',
            arrivalIso: '2026-05-16T09:27:00Z',
            arrivalTime: '09:27',
            routeScore: 94,
            robustScore: 93.2,
            hasThunderCap: false,
            reachesNightfall: false,
            segmentScores: [95, 94, 93],
            segmentReasons: ['Perfekt', 'Rückenwind', 'Trocken'],
            summaryDeltaText: '+42 Pkt.',
        },
        {
            departureIso: '2026-05-16T14:00:00Z',
            departureTime: '14:00',
            arrivalIso: '2026-05-16T15:12:00Z',
            arrivalTime: '15:12',
            routeScore: 42,
            robustScore: 44.0,
            hasThunderCap: true,
            reachesNightfall: false,
            segmentScores: [40, 42, 44],
            segmentReasons: ['Gewitter', 'Regen', 'Böen'],
            summaryDeltaText: 'Tiefstwert im Zeitfenster',
        },
    ]

    const mockSummary: OptimalWindowSummary = {
        bestDeparture: '08:15',
        bestDepartureIso: '2026-05-16T08:15:00Z',
        bestScore: 94,
        stableWindow: '08:00 – 09:30 Uhr',
        advantageVsWorst: '+52 Punkte ggü. 14:00 Uhr (umgeht Gewitterfront)',
        worstDeparture: '14:00',
        worstScore: 42,
    }

    it('renders the header, optimum badge and summary card', () => {
        render(
            <DepartureTimeSlider
                slots={mockSlots}
                recommendedSlotIndex={1}
                selectedSlotIndex={1}
                optimalWindowSummary={mockSummary}
                initialDepartureIndex={0}
                onSelectSlot={vi.fn()}
            />,
        )

        expect(screen.getByText('Abfahrtszeit-Optimierung')).toBeInTheDocument()
        expect(screen.getByText('Optimum: 08:15 Uhr (Score 94)')).toBeInTheDocument()
        expect(screen.getByText('08:00 – 09:30 Uhr')).toBeInTheDocument()
        expect(screen.getByText('+52 Punkte ggü. 14:00 Uhr (umgeht Gewitterfront)')).toBeInTheDocument()
    })

    it('allows clicking on a slot bar to trigger slot selection', async () => {
        const handleSelect = vi.fn()
        const user = userEvent.setup()

        render(
            <DepartureTimeSlider
                slots={mockSlots}
                recommendedSlotIndex={1}
                selectedSlotIndex={0}
                optimalWindowSummary={mockSummary}
                initialDepartureIndex={0}
                onSelectSlot={handleSelect}
            />,
        )

        const optionBars = screen.getAllByRole('option')
        expect(optionBars).toHaveLength(3)

        await user.click(optionBars[1]!)
        expect(handleSelect).toHaveBeenCalledWith(1)
    })

    it('displays delta comparison when slot different from initial is selected', () => {
        render(
            <DepartureTimeSlider
                slots={mockSlots}
                recommendedSlotIndex={1}
                selectedSlotIndex={1}
                optimalWindowSummary={mockSummary}
                initialDepartureIndex={0}
                onSelectSlot={vi.fn()}
            />,
        )

        // 94 - 82 = +12
        expect(screen.getByText(/Start um 08:15 statt 08:00/)).toBeInTheDocument()
        expect(screen.getByText('+12 Punkte')).toBeInTheDocument()
    })

    it('toggles 2D Raum-Zeit Carpet Plot when clicked', async () => {
        const user = userEvent.setup()

        render(
            <DepartureTimeSlider
                slots={mockSlots}
                recommendedSlotIndex={1}
                selectedSlotIndex={1}
                optimalWindowSummary={mockSummary}
                initialDepartureIndex={0}
                onSelectSlot={vi.fn()}
            />,
        )

        const toggleBtn = screen.getByRole('button', { name: '2D Raum-Zeit-Matrix' })
        expect(screen.queryByText(/2D Raum-Zeit-Matrix \(Carpet Plot\)/)).not.toBeInTheDocument()

        await user.click(toggleBtn)
        expect(screen.getByText(/2D Raum-Zeit-Matrix \(Carpet Plot\)/)).toBeInTheDocument()
    })

    it('renders thunderstorm indicator for slots with thunder cap', () => {
        render(
            <DepartureTimeSlider
                slots={mockSlots}
                recommendedSlotIndex={1}
                selectedSlotIndex={2}
                optimalWindowSummary={mockSummary}
                initialDepartureIndex={0}
                onSelectSlot={vi.fn()}
            />,
        )

        expect(screen.getByText(/Gewitterrisiko \(Score max\. 50\)/)).toBeInTheDocument()
    })

    it('renders best marker pin on recommended slot and active pin on selected slot', () => {
        render(
            <DepartureTimeSlider
                slots={mockSlots}
                recommendedSlotIndex={1}
                selectedSlotIndex={0}
                optimalWindowSummary={mockSummary}
                initialDepartureIndex={0}
                onSelectSlot={vi.fn()}
            />,
        )

        expect(screen.getByText('BEST')).toBeInTheDocument()
        expect(screen.getByText('AKTIV')).toBeInTheDocument()
    })
})
